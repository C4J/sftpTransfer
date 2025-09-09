package com.commander4j.crypto;


import java.nio.file.Path;

public class Demo {
    public static void main(String[] args) throws Exception {
        Path priv = Path.of("./ssh/id_ed25519.pem");
        Path pub  = Path.of("./ssh/id_ed25519.pub");

        char[] pass = "change_me_strong_passphrase".toCharArray();

        KeyGenUtil.generateEd25519AndWriteEncryptedPKCS8(
                priv,
                pub,
                pass,
                "dave@your-host"
        );

        // Optional: zero the passphrase when done.
        java.util.Arrays.fill(pass, '\0');

        System.out.println("Wrote:");
        System.out.println("  Private (encrypted PKCS#8): " + priv);
        System.out.println("  Public (OpenSSH):           " + pub);
    }
}
