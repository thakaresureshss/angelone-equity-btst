package com.trade.algotrade.utils;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SymmetricEncryption {

    private static final String AES = "AES";

    @Value("${application.symmetric.key}")
    private String symmetricKey;

    public SecretKey getKey() throws Exception {
//		 *** SecretKey generation code ***
//		 KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
//	     keyGenerator.init(128); // block size is 128bits
//	     SecretKey secretKey = keyGenerator.generateKey();
//	     String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
//		 System.out.println(encodedKey);

        byte[] decodedKey = Base64.getDecoder().decode(symmetricKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, AES);
    }

    public String encrypt(String plainText) throws Exception {
        byte[] plainTextByte = plainText.getBytes();
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, getKey());
        byte[] encryptedByte = cipher.doFinal(plainTextByte);
        Base64.Encoder encoder = Base64.getEncoder();
        String encryptedText = encoder.encodeToString(encryptedByte);
        return encryptedText;
    }

    public String decrypt(String encryptedText) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] encryptedTextByte = decoder.decode(encryptedText);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, getKey());
        byte[] decryptedByte = cipher.doFinal(encryptedTextByte);
        String decryptedText = new String(decryptedByte);
        return decryptedText;
    }
}
