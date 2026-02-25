package com.samjenkins.auth_service.service;

import com.samjenkins.auth_service.dto.AuthDtos.AuthResponse;
import com.samjenkins.auth_service.dto.AuthDtos.LoginRequest;
import com.samjenkins.auth_service.dto.AuthDtos.LogoutRequest;
import com.samjenkins.auth_service.dto.AuthDtos.RefreshRequest;
import com.samjenkins.auth_service.dto.AuthDtos.RegisterRequest;
import com.samjenkins.auth_service.entity.UserEntity;
import com.samjenkins.auth_service.repository.UserRepository;
import java.util.Locale;

import com.samjenkins.auth_service.util.ServiceConstants;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
        UserRepository users,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        LoginAttemptService loginAttemptService
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req, String clientIp, String userAgent) {
        String normalizedEmail = normalizeEmail(req.email());
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ServiceConstants.EMAIL_IN_USE);
        }

        UserEntity user = new UserEntity();
        user.setEmail(normalizedEmail);
        user.setDisplayName(req.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        try {
            users.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ServiceConstants.EMAIL_IN_USE);
        }

        String access = jwtService.issueAccessToken(user);
        var issued = refreshTokenService.issue(user.getId(), clientIp, userAgent);
        return new AuthResponse(access, issued.rawToken());
    }

    @Transactional
    public AuthResponse login(LoginRequest req, String clientIp, String userAgent) {
        String normalizedEmail = normalizeEmail(req.email());
        String rateLimitKey = normalizedEmail + "|" + clientIp;
        if (loginAttemptService.isBlocked(rateLimitKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ServiceConstants.TOO_MANY_LOGIN_ATTEMPTS);
        }

        UserEntity user = users.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> {
                loginAttemptService.recordFailure(rateLimitKey);
                return unauthorized(ServiceConstants.INVALID_CREDENTIALS);
            });

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(rateLimitKey);
            throw unauthorized(ServiceConstants.INVALID_CREDENTIALS);
        }

        loginAttemptService.recordSuccess(rateLimitKey);
        String access = jwtService.issueAccessToken(user);
        var issued = refreshTokenService.issue(user.getId(), clientIp, userAgent);
        return new AuthResponse(access, issued.rawToken());
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req, String clientIp, String userAgent) {
        try {
            var issued = refreshTokenService.rotate(req.refreshToken(), clientIp, userAgent);
            UserEntity user = users.findById(issued.userId())
                .orElseThrow(() -> unauthorized(ServiceConstants.INVALID_TOKEN));
            String access = jwtService.issueAccessToken(user);
            return new AuthResponse(access, issued.rawToken());
        } catch (IllegalArgumentException ex) {
            throw unauthorized(ServiceConstants.INVALID_TOKEN);
        }
    }

    @Transactional
    public void logout(LogoutRequest req) {
        try {
            refreshTokenService.revoke(req.refreshToken());
        } catch (IllegalArgumentException ex) {
            throw unauthorized(ServiceConstants.INVALID_TOKEN);
        }
    }

    public ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
