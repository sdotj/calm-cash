package com.samjenkins.auth_service.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenHashingTest {

    @Test
    void sameInputProducesSameHash() {
        String raw = "example-refresh-token-value";
        String first = TokenHashing.sha256Base64(raw);
        String second = TokenHashing.sha256Base64(raw);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentInputProducesDifferentHash() {
        String first = TokenHashing.sha256Base64("token-a");
        String second = TokenHashing.sha256Base64("token-b");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashLengthMatchesSha256Base64() {
        String hash = TokenHashing.sha256Base64("token");

        assertThat(hash).hasSize(44);
    }

    @Test
    void nullInputThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> TokenHashing.sha256Base64(null));
    }
}
