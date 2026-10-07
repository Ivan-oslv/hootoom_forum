package com.hootoom.forum.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** 使用 AES-256-GCM 加密 Mail Outbox 中的隐私数据，输出格式为 version|iv|ciphertext。 */
@Component
public class EnvelopeEncryptionService {
    public static final String KEY_VERSION = "v1";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final byte[] key;
    private final SecureRandom secureRandom = new SecureRandom();

    public EnvelopeEncryptionService(@Value("${app.mail.outbox-encryption-key:}") String encodedKey) {
        this.key = decodeKey(encodedKey);
    }

    public byte[] encrypt(String plaintext) {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Mail outbox encryption failed", exception);
        }
    }

    public String decrypt(byte[] encrypted, String keyVersion) {
        if (!KEY_VERSION.equals(keyVersion)) {
            throw new IllegalStateException("Unsupported mail encryption key version: " + keyVersion);
        }
        if (encrypted == null || encrypted.length <= IV_BYTES) {
            throw new IllegalStateException("Invalid encrypted mail payload");
        }
        byte[] iv = new byte[IV_BYTES];
        byte[] ciphertext = new byte[encrypted.length - IV_BYTES];
        ByteBuffer buffer = ByteBuffer.wrap(encrypted);
        buffer.get(iv);
        buffer.get(ciphertext);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Mail outbox decryption failed", exception);
        }
    }

    private byte[] decodeKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("MAIL_OUTBOX_ENCRYPTION_KEY is required for mail tasks");
        }
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != 32) {
                throw new IllegalStateException("MAIL_OUTBOX_ENCRYPTION_KEY must decode to 32 bytes");
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MAIL_OUTBOX_ENCRYPTION_KEY must be valid Base64", exception);
        }
    }
}
