package com.applicationtracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Service for encrypting and decrypting sensitive strings (such as OAuth refresh tokens)
 * using AES-256-GCM authenticated encryption.
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(@Value("${app.security.encryption-key:}") String configuredKey) {
        this.secretKey = deriveSecretKey(configuredKey);
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * The resulting string contains Base64 encoded [12-byte IV + ciphertext + 16-byte auth tag].
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt token with AES-GCM", e);
        }
    }

    /**
     * Decrypts AES-256-GCM encrypted Base64 string.
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            if (decoded.length < IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
                throw new IllegalArgumentException("Invalid encrypted payload size");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherText);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt AES-GCM payload. Data may have been tampered with.", e);
        }
    }

    private SecretKey deriveSecretKey(String configuredKey) {
        try {
            byte[] keyBytes;
            if (configuredKey == null || configuredKey.isBlank()) {
                // Fallback for local development if not configured
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest("application-tracker-default-dev-key".getBytes(StandardCharsets.UTF_8));
            } else if (configuredKey.length() == 64 && isHex(configuredKey)) {
                keyBytes = HexFormat.of().parseHex(configuredKey);
            } else if (configuredKey.length() == 44 && isBase64(configuredKey)) {
                keyBytes = Base64.getDecoder().decode(configuredKey);
            } else {
                // Hash any generic secret passphrase to a secure 256-bit key
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(configuredKey.getBytes(StandardCharsets.UTF_8));
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AES encryption key", e);
        }
    }

    private boolean isHex(String s) {
        return s.matches("^[0-9a-fA-F]+$");
    }

    private boolean isBase64(String s) {
        try {
            Base64.getDecoder().decode(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
