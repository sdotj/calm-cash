package com.samjenkins.auth_service.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptState state = attempts.get(key);
        if (state == null || state.blockedUntil == null) {
            return false;
        }
        Instant now = Instant.now();
        if (now.isAfter(state.blockedUntil)) {
            attempts.remove(key, state);
            return false;
        }
        return true;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (ignored, existing) -> {
            Instant now = Instant.now();
            if (existing == null || existing.windowStart == null || now.isAfter(existing.windowStart.plus(WINDOW))) {
                return new AttemptState(1, now, null);
            }

            int failures = existing.failures + 1;
            Instant blockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : existing.blockedUntil;
            return new AttemptState(failures, existing.windowStart, blockedUntil);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private record AttemptState(int failures, Instant windowStart, Instant blockedUntil) {}
}
