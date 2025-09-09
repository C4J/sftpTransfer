package com.commander4j.crypto;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcaPKCS8EncryptedPrivateKeyInfoBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.util.Base64;
import java.util.Set;

public final class KeyGenUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private KeyGenUtil() {}

    /**
     * Generates an Ed25519 key pair, writes the private key as an encrypted PKCS#8 PEM
     * ("BEGIN ENCRYPTED PRIVATE KEY", OpenSSL-compatible AES-256-CBC),
     * and writes the public key in OpenSSH format (ssh-ed25519 ...).
     */
    public static void generateEd25519AndWriteEncryptedPKCS8(
            Path privateKeyPemPath,
            Path publicKeyOpenSsh,
            char[] passphrase,
            String publicKeyComment
    ) throws GeneralSecurityException, IOException {

        // 1) Generate Ed25519 key pair (no keysize/init needed)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = kpg.generateKeyPair();

        // 2) Build encryptor (AES-256-CBC), using non-deprecated setPassword(...)
        final OutputEncryptor encryptor;
        try {
            encryptor = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .setRandom(new SecureRandom())
                    .setPassword(passphrase)   // ✅ modern API
                    .build();
        } catch (OperatorCreationException e) {
            throw new GeneralSecurityException("Failed to create PKCS#8 encryptor", e);
        }

        // 3) Encrypt the private key into PKCS#8
        PKCS8EncryptedPrivateKeyInfo encryptedInfo =
                new JcaPKCS8EncryptedPrivateKeyInfoBuilder(kp.getPrivate()).build(encryptor);

        // 4) Write PEM ("BEGIN ENCRYPTED PRIVATE KEY")
        try (Writer fw = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(privateKeyPemPath,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE),
                StandardCharsets.UTF_8));
             JcaPEMWriter pemWriter = new JcaPEMWriter(fw)) {
            pemWriter.writeObject(encryptedInfo);
        }

        // chmod 600 on POSIX
        try {
            if (Files.getFileAttributeView(privateKeyPemPath, PosixFileAttributeView.class) != null) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(privateKeyPemPath, perms);
            }
        } catch (UnsupportedOperationException ignored) { }

        // 5) Write OpenSSH public key line
        byte[] spki = kp.getPublic().getEncoded();
        byte[] rawEd25519 = SubjectPublicKeyInfo.getInstance(spki).getPublicKeyData().getBytes();
        String opensshLine = buildOpenSshEd25519Line(rawEd25519,
                (publicKeyComment == null ? "" : publicKeyComment).trim());

        Files.writeString(publicKeyOpenSsh, opensshLine + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static String buildOpenSshEd25519Line(byte[] rawPubKey32, String comment) throws IOException {
        byte[] alg = "ssh-ed25519".getBytes(StandardCharsets.US_ASCII);

        byte[] payload;
        try (var bos = new java.io.ByteArrayOutputStream();
             var dos = new java.io.DataOutputStream(bos)) {
            dos.writeInt(alg.length);
            dos.write(alg);
            dos.writeInt(rawPubKey32.length);
            dos.write(rawPubKey32);
            dos.flush();
            payload = bos.toByteArray();
        }

        String b64 = Base64.getEncoder().encodeToString(payload);
        return (comment == null || comment.isBlank())
                ? "ssh-ed25519 " + b64
                : "ssh-ed25519 " + b64 + " " + comment;
    }

    // Demo
    public static void main(String[] args) throws Exception {
        Path priv = Path.of(System.getProperty("user.home"), ".ssh", "id_ed25519.pem");
        Path pub  = Path.of(System.getProperty("user.home"), ".ssh", "id_ed25519.pub");

        // Assign your passphrase here (or read from args/Console)
        char[] pass = "change_me_strong_passphrase".toCharArray();

        generateEd25519AndWriteEncryptedPKCS8(
                priv,
                pub,
                pass,
                "dave@your-host"
        );

        java.util.Arrays.fill(pass, '\0');

        System.out.println("Wrote:");
        System.out.println("  Private (encrypted PKCS#8): " + priv);
        System.out.println("  Public (OpenSSH):           " + pub);
    }
}
