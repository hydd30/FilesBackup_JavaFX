package com.example.project.entity;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

public class Crypto {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;
    private static final String SALT = "BackupSalt";
    private static final int ITERATIONS = 10000;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static class CryptoException extends RuntimeException {
        public CryptoException(String message) {
            super(message);
        }

        public CryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class KeyIvPair {
        byte[] key;
        byte[] iv;

        public KeyIvPair(byte[] key, byte[] iv) {
            this.key = key;
            this.iv = iv;
        }
    }

    private KeyIvPair deriveKey(String password) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT.getBytes(), ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] key = factory.generateSecret(spec).getEncoded();

            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            return new KeyIvPair(key, iv);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("密钥生成失败", e);
        } catch (InvalidKeySpecException e) {
            throw new CryptoException("密钥生成失败", e);
        }
    }

    public byte[] encrypt(byte[] data, String password) {
        try {
            KeyIvPair keyIv = deriveKey(password);

            SecretKeySpec keySpec = new SecretKeySpec(keyIv.key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(keyIv.iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(data);
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(keyIv.iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return combined;
        } catch (Exception e) {
            throw new CryptoException("加密失败", e);
        }
    }

    public byte[] decrypt(byte[] encrypted, String password) {
        try {
            if(encrypted.length < IV_LENGTH) {
                throw new CryptoException("加密数据格式无效");
            }

            byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_LENGTH);
            byte[] encryptedData = Arrays.copyOfRange(encrypted, IV_LENGTH, encrypted.length);

            KeyIvPair keyIv = deriveKey(password);
            keyIv.iv = iv;

            SecretKeySpec keySpec = new SecretKeySpec(keyIv.key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(keyIv.iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new CryptoException("解密失败", e);
        }
    }

    public String decryptString(String encryptedBase64, String password) {
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
        byte[] decrypted = decrypt(encrypted, password);
        return new String(decrypted);
    }

    public String encryptString(String data, String password) {
        byte[] encrypted = encrypt(data.getBytes(), password);
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
