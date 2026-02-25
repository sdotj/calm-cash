package com.samjenkins.auth_service.service;

import com.samjenkins.auth_service.config.JwtProperties;
import com.samjenkins.auth_service.entity.RefreshTokenEntity;
import com.samjenkins.auth_service.repository.RefreshTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repo;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties("issuer", "audience", "0123456789abcdef0123456789abcdef", 15, 14);
        service = new RefreshTokenService(repo, props);
    }

    @Test
    void issuePersistsHashedTokenAndReturnsRawToken() {
        UUID userId = UUID.randomUUID();
        when(repo.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> {
            RefreshTokenEntity ent = invocation.getArgument(0);
            if (ent.getId() == null) {
                ent.setId(UUID.randomUUID());
            }
            return ent;
        });

        RefreshTokenService.IssuedRefreshToken issued = service.issue(userId, "127.0.0.1", "JUnit");

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(repo).save(captor.capture());
        RefreshTokenEntity saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTokenHash()).isEqualTo(TokenHashing.sha256Base64(issued.rawToken()));
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(13, ChronoUnit.DAYS));
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getUserAgent()).isEqualTo("JUnit");
        assertThat(issued.userId()).isEqualTo(userId);
        assertThat(issued.rawToken()).isNotBlank();
    }

    @Test
    void rotateThrowsOnUnknownToken() {
        when(repo.findByTokenHash(any(String.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.rotate("missing-token", "127.0.0.1", "JUnit"));
    }

    @Test
    void rotateThrowsOnRevokedOrExpiredToken() {
        RefreshTokenEntity revoked = new RefreshTokenEntity();
        revoked.setRevokedAt(Instant.now());
        revoked.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(repo.findByTokenHash(eq(TokenHashing.sha256Base64("revoked")))).thenReturn(Optional.of(revoked));

        assertThrows(IllegalArgumentException.class, () -> service.rotate("revoked", "127.0.0.1", "JUnit"));

        RefreshTokenEntity expired = new RefreshTokenEntity();
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(repo.findByTokenHash(eq(TokenHashing.sha256Base64("expired")))).thenReturn(Optional.of(expired));

        assertThrows(IllegalArgumentException.class, () -> service.rotate("expired", "127.0.0.1", "JUnit"));
    }

    @Test
    void rotateIssuesNewTokenAndRevokesCurrentToken() {
        UUID userId = UUID.randomUUID();
        RefreshTokenEntity current = new RefreshTokenEntity();
        current.setId(UUID.randomUUID());
        current.setUserId(userId);
        current.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(repo.findByTokenHash(eq(TokenHashing.sha256Base64("current-raw-token")))).thenReturn(Optional.of(current));
        when(repo.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> {
            RefreshTokenEntity ent = invocation.getArgument(0);
            if (ent.getId() == null) {
                ent.setId(UUID.randomUUID());
            }
            return ent;
        });

        RefreshTokenService.IssuedRefreshToken issued =
            service.rotate("current-raw-token", "127.0.0.1", "JUnit");

        assertThat(issued.userId()).isEqualTo(userId);
        assertThat(issued.tokenId()).isNotNull();
        assertThat(current.getRevokedAt()).isNotNull();
        assertThat(current.getReplacedByTokenId()).isEqualTo(issued.tokenId());
        verify(repo, atLeastOnce()).save(any(RefreshTokenEntity.class));
    }

    @Test
    void revokeMarksTokenAsRevoked() {
        RefreshTokenEntity current = new RefreshTokenEntity();
        current.setId(UUID.randomUUID());
        current.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(repo.findByTokenHash(eq(TokenHashing.sha256Base64("to-revoke")))).thenReturn(Optional.of(current));

        service.revoke("to-revoke");

        assertThat(current.getRevokedAt()).isNotNull();
        verify(repo).save(current);
    }
}
