package com.samjenkins.auth_service.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterFiveFailuresAndUnblocksOnSuccess() {
        LoginAttemptService service = new LoginAttemptService();
        String key = "user@example.com|127.0.0.1";

        for (int i = 0; i < 4; i++) {
            service.recordFailure(key);
            assertThat(service.isBlocked(key)).isFalse();
        }

        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isTrue();

        service.recordSuccess(key);
        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void blockedEntryExpiresAndIsRemoved() throws Exception {
        LoginAttemptService service = new LoginAttemptService();
        String key = "user@example.com|127.0.0.1";

        Field attemptsField = LoginAttemptService.class.getDeclaredField("attempts");
        attemptsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> attempts = (Map<String, Object>) attemptsField.get(service);

        Class<?> attemptStateClass = Class.forName(
            "com.samjenkins.auth_service.service.LoginAttemptService$AttemptState"
        );
        Constructor<?> ctor = attemptStateClass.getDeclaredConstructor(int.class, Instant.class, Instant.class);
        ctor.setAccessible(true);
        Object expiredState = ctor.newInstance(5, Instant.now().minusSeconds(3600), Instant.now().minusSeconds(60));
        attempts.put(key, expiredState);

        assertThat(service.isBlocked(key)).isFalse();
        assertThat(attempts.containsKey(key)).isFalse();
    }

    @Test
    void failuresOutsideWindowResetCounter() throws Exception {
        LoginAttemptService service = new LoginAttemptService();
        String key = "user@example.com|127.0.0.1";

        service.recordFailure(key);

        Field attemptsField = LoginAttemptService.class.getDeclaredField("attempts");
        attemptsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> attempts = (Map<String, Object>) attemptsField.get(service);

        Class<?> attemptStateClass = Class.forName(
            "com.samjenkins.auth_service.service.LoginAttemptService$AttemptState"
        );
        Constructor<?> ctor = attemptStateClass.getDeclaredConstructor(int.class, Instant.class, Instant.class);
        ctor.setAccessible(true);
        Object oldState = ctor.newInstance(4, Instant.now().minusSeconds(3600), null);
        attempts.put(key, oldState);

        service.recordFailure(key);

        Field failuresField = attemptStateClass.getDeclaredField("failures");
        failuresField.setAccessible(true);
        int failures = failuresField.getInt(attempts.get(key));
        assertThat(failures).isEqualTo(1);
        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void blockedEntryStaysBlockedBeforeExpiry() throws Exception {
        LoginAttemptService service = new LoginAttemptService();
        String key = "user@example.com|127.0.0.1";

        Field attemptsField = LoginAttemptService.class.getDeclaredField("attempts");
        attemptsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> attempts = (Map<String, Object>) attemptsField.get(service);

        Class<?> attemptStateClass = Class.forName(
            "com.samjenkins.auth_service.service.LoginAttemptService$AttemptState"
        );
        Constructor<?> ctor = attemptStateClass.getDeclaredConstructor(int.class, Instant.class, Instant.class);
        ctor.setAccessible(true);
        Object blockedState = ctor.newInstance(5, Instant.now(), Instant.now().plusSeconds(120));
        attempts.put(key, blockedState);

        assertThat(service.isBlocked(key)).isTrue();
        assertThat(service.isBlocked(key)).isTrue();
    }
}
