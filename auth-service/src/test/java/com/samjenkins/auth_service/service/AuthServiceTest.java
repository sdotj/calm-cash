package com.samjenkins.auth_service.service;

import com.samjenkins.auth_service.dto.AuthDtos.AuthResponse;
import com.samjenkins.auth_service.dto.AuthDtos.LoginRequest;
import com.samjenkins.auth_service.dto.AuthDtos.LogoutRequest;
import com.samjenkins.auth_service.dto.AuthDtos.RefreshRequest;
import com.samjenkins.auth_service.dto.AuthDtos.RegisterRequest;
import com.samjenkins.auth_service.entity.UserEntity;
import com.samjenkins.auth_service.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private LoginAttemptService loginAttemptService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, passwordEncoder, jwtService, refreshTokenService, loginAttemptService);
    }

    @Test
    void registerCreatesUserAndReturnsTokens() {
        RegisterRequest req = new RegisterRequest("User@Example.com", "super-secure-password-1", " Test User ");
        UUID userId = UUID.randomUUID();
        String hashedPassword = "encoded-password";
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        String ip = "127.0.0.1";
        String ua = "JUnit";

        when(users.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn(hashedPassword);
        when(jwtService.issueAccessToken(any(UserEntity.class))).thenReturn(accessToken);
        when(refreshTokenService.issue(eq(userId), eq(ip), eq(ua)))
            .thenReturn(new RefreshTokenService.IssuedRefreshToken(UUID.randomUUID(), userId, refreshToken));
        when(users.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        AuthResponse response = authService.register(req, ip, ua);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo(hashedPassword);
        assertThat(savedUser.getDisplayName()).isEqualTo("Test User");
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
    }

    @Test
    void registerDuplicateEmailThrowsConflict() {
        RegisterRequest req = new RegisterRequest("user@example.com", "super-secure-password-1", "User");
        when(users.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.register(req, "127.0.0.1", "JUnit")
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
        verify(users, never()).save(any(UserEntity.class));
    }

    @Test
    void registerDataIntegrityViolationThrowsConflict() {
        RegisterRequest req = new RegisterRequest("user@example.com", "super-secure-password-1", "User");
        when(users.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hash");
        when(users.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.register(req, "127.0.0.1", "JUnit")
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void loginBlockedReturnsTooManyRequests() {
        LoginRequest req = new LoginRequest("user@example.com", "super-secure-password-1");
        when(loginAttemptService.isBlocked("user@example.com|127.0.0.1")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.login(req, "127.0.0.1", "JUnit")
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(users, never()).findByEmailIgnoreCase(any(String.class));
    }

    @Test
    void loginUserNotFoundRecordsFailureAndThrowsUnauthorized() {
        LoginRequest req = new LoginRequest("user@example.com", "super-secure-password-1");
        String key = "user@example.com|127.0.0.1";
        when(loginAttemptService.isBlocked(key)).thenReturn(false);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.login(req, "127.0.0.1", "JUnit")
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(loginAttemptService).recordFailure(key);
    }

    @Test
    void loginInvalidPasswordRecordsFailureAndThrowsUnauthorized() {
        LoginRequest req = new LoginRequest("user@example.com", "wrong-password-111");
        String key = "user@example.com|127.0.0.1";
        UserEntity user = new UserEntity();
        user.setPasswordHash("stored-hash");

        when(loginAttemptService.isBlocked(key)).thenReturn(false);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), "stored-hash")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.login(req, "127.0.0.1", "JUnit")
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(loginAttemptService).recordFailure(key);
    }

    @Test
    void loginSuccessRecordsSuccessAndReturnsTokens() {
        LoginRequest req = new LoginRequest("User@Example.com", "super-secure-password-1");
        String key = "user@example.com|127.0.0.1";
        UserEntity user = new UserEntity();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        user.setPasswordHash("stored-hash");

        when(loginAttemptService.isBlocked(key)).thenReturn(false);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), "stored-hash")).thenReturn(true);
        when(jwtService.issueAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.issue(userId, "127.0.0.1", "JUnit"))
            .thenReturn(new RefreshTokenService.IssuedRefreshToken(UUID.randomUUID(), userId, "refresh-token"));

        AuthResponse response = authService.login(req, "127.0.0.1", "JUnit");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(loginAttemptService).recordSuccess(key);
    }

    @Test
    void refreshInvalidTokenThrowsUnauthorized() {
        when(refreshTokenService.rotate("invalid", "127.0.0.1", "JUnit"))
            .thenThrow(new IllegalArgumentException("Invalid refresh token"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.refresh(new RefreshRequest("invalid"), "127.0.0.1", "JUnit")
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void refreshWithMissingUserThrowsUnauthorized() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenService.rotate("raw-token", "127.0.0.1", "JUnit"))
            .thenReturn(new RefreshTokenService.IssuedRefreshToken(UUID.randomUUID(), userId, "new-token"));
        when(users.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.refresh(new RefreshRequest("raw-token"), "127.0.0.1", "JUnit")
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void logoutInvalidTokenThrowsUnauthorized() {
        doThrow(new IllegalArgumentException("Invalid")).when(refreshTokenService).revoke("bad-token");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.logout(new LogoutRequest("bad-token"))
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}
