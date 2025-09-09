package com.commander4j.util;

/**
 * @author David Garratt
 * 
 * Project Name : Commander4j
 * 
 * Filename     : JCipher.java
 * 
 * Package Name : com.commander4j.sftp
 * 
 * License      : GNU General Public License
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * http://www.commander4j.com/website/license.html.
 * 
 */

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class JCipher {
	private String encryptionKey;

	public JCipher(String encryptionKey)
	{
		this.encryptionKey = encryptionKey;
	}
	
	public String conditionalDecrypt(String plainText,String yesNo)
	{
		String result = plainText;
		
		if (yesNo.toLowerCase().equals("true"))
		{
			try
			{
				result = decrypt(plainText);
			}
			catch (Exception ex)
			{
				result = ex.getMessage();
			}
		}
		
		return result;
	}
	
	public String conditionalDecrypt(String plainText,String yesNo,String defaultValue)
	{
		String result = conditionalDecrypt(plainText,yesNo);
		
		if (result.equals(""))
		{
			result = defaultValue;
		}
		
		return result;
	}
	
	public String conditionalEncrypt(String plainText,String yesNo)
	{
		String result = plainText;
		
		if (yesNo.toLowerCase().equals("true"))
		{
			try
			{
				result = encrypt(plainText);
			}
			catch (Exception ex)
			{
				result = ex.getMessage();
			}
		}
		
		return result;
	}
	
	public String conditionalEncrypt(String plainText,String yesNo,String defaultValue)
	{
		String result = plainText;
		
		if (plainText.equals(""))
		{
			plainText = defaultValue;
		}
			
		result = conditionalEncrypt(plainText,yesNo);
		
		return result;
	}

	public String encrypt(String plainText) throws Exception
	{
		Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
		byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());

		return Base64.encodeBase64String(encryptedBytes);
	}

	public String decrypt(String encrypted) throws Exception
	{
		Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
		byte[] plainBytes = cipher.doFinal(Base64.decodeBase64(encrypted));

		return new String(plainBytes);
	}

	private Cipher getCipher(int cipherMode) throws Exception
	{
		String encryptionAlgorithm = "AES";
		SecretKeySpec keySpecification = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), encryptionAlgorithm);
		Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
		cipher.init(cipherMode, keySpecification);

		return cipher;
	}

	public String encode(String value)
	{
		String result = "";

        try
        {
        	result = encrypt(value);
        }
        catch (Exception ex)
        {
        	result = "";
        }
		return result;
	}

	public String decode(String value)
	{
		String result = "";
	
		try
		{
			result = decrypt(value);
		} catch (Exception ex)
		{
			result = "";
		}
		return result;
	}

}