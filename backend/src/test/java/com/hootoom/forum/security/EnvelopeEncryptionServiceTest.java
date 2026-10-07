package com.hootoom.forum.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class EnvelopeEncryptionServiceTest {
    @Test
    void encryptsWithRandomIvAndDecryptsWithoutPersistingPlaintext() {
        String key = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef"
                .getBytes(StandardCharsets.UTF_8));
        EnvelopeEncryptionService service = new EnvelopeEncryptionService(key);

        byte[] first = service.encrypt("marked-user@example.com");
        byte[] second = service.encrypt("marked-user@example.com");

        assertThat(first).isNotEqualTo(second);
        assertThat(new String(first, StandardCharsets.UTF_8)).doesNotContain("marked-user@example.com");
        assertThat(service.decrypt(first, EnvelopeEncryptionService.KEY_VERSION))
                .isEqualTo("marked-user@example.com");
    }
}
