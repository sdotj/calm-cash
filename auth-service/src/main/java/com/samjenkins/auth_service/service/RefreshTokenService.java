package com.samjenkins.auth_service.service;

import com.samjenkins.auth_service.config.JwtProperties;
import com.samjenkins.auth_service.entity.RefreshTokenEntity;
import com.samjenkins.auth_service.repository.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final JwtProperties props;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo, JwtProperties props) {
        this.repo = repo;
        this.props = props;
    }

    public record IssuedRefreshToken(UUID tokenId, UUID userId, String rawToken) {}

    public IssuedRefreshToken issue(UUID userId, String ip, String userAgent) {
        String raw = generateRawToken();
        String hash = TokenHashing.sha256Base64(raw);

        RefreshTokenEntity ent = new RefreshTokenEntity();
        ent.setUserId(userId);
        ent.setTokenHash(hash);
        ent.setExpiresAt(Instant.now().plus(props.refreshTokenDays(), ChronoUnit.DAYS));
        ent.setIpAddress(ip);
        ent.setUserAgent(userAgent);

        repo.save(ent);
        return new IssuedRefreshToken(ent.getId(), userId, raw);
    }

    @Transactional
    public IssuedRefreshToken rotate(String presentedRawToken, String ip, String userAgent) {
        String presentedHash = TokenHashing.sha256Base64(presentedRawToken);

        RefreshTokenEntity current = repo.findByTokenHash(presentedHash)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (current.isRevoked() || current.isExpired()) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        IssuedRefreshToken next = issue(current.getUserId(), ip, userAgent);

        current.setRevokedAt(Instant.now());
        current.setReplacedByTokenId(next.tokenId());
        repo.save(current);

        return next;
    }

    @Transactional
    public void revoke(String presentedRawToken) {
        String presentedHash = TokenHashing.sha256Base64(presentedRawToken);

        RefreshTokenEntity current = repo.findByTokenHash(presentedHash)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!current.isRevoked()) {
            current.setRevokedAt(Instant.now());
            repo.save(current);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
