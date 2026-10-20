package com.applicationtracker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private final EncryptionService encryptionService =
            new EncryptionService("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

    @Test
    @DisplayName("Should successfully encrypt and decrypt a plaintext string")
    void testEncryptDecryptRoundTrip() {
        String plainText = "1//04test_google_refresh_token_xyz_123456789";
        String encrypted = encryptionService.encrypt(plainText);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plainText);

        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should produce different ciphertexts for the same plaintext due to random IV")
    void testRandomIvProducesDifferentCiphertext() {
        String plainText = "sample-secret-token";
        String encrypted1 = encryptionService.encrypt(plainText);
        String encrypted2 = encryptionService.encrypt(plainText);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plainText);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should fail decryption when ciphertext is tampered with (GCM authentication check)")
    void testTamperedCiphertextThrowsException() {
        String plainText = "secure_refresh_token";
        String encrypted = encryptionService.encrypt(plainText);

        byte[] decoded = Base64.getDecoder().decode(encrypted);
        // Flip one byte in the ciphertext payload
        decoded[decoded.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(decoded);

        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to decrypt");
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void testNullHandling() {
        assertThat(encryptionService.encrypt(null)).isNull();
        assertThat(encryptionService.decrypt(null)).isNull();
    }
}
