package com.samjenkins.auth_service.controller;

import com.samjenkins.auth_service.dto.AuthDtos.*;
import com.samjenkins.auth_service.entity.UserEntity;
import com.samjenkins.auth_service.repository.UserRepository;
import com.samjenkins.auth_service.service.JwtService;
import com.samjenkins.auth_service.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
        UserRepository users,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        RefreshTokenService refreshTokenService
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        UserEntity u = new UserEntity();
        u.setEmail(req.email().toLowerCase());
        u.setDisplayName(req.displayName());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        users.save(u);

        String access = jwtService.issueAccessToken(u);
        var issued = refreshTokenService.issue(u.getId(), clientIp(http), userAgent(http));
        return new AuthResponse(access, issued.rawToken());
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        UserEntity u = users.findByEmailIgnoreCase(req.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String access = jwtService.issueAccessToken(u);
        var issued = refreshTokenService.issue(u.getId(), clientIp(http), userAgent(http));
        return new AuthResponse(access, issued.rawToken());
    }

    @PostMapping("/auth/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        var issued = refreshTokenService.rotate(req.refreshToken(), clientIp(http), userAgent(http));
        UserEntity u = users.findById(issued.userId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        String access = jwtService.issueAccessToken(u);
        return new AuthResponse(access, issued.rawToken());
    }

    @PostMapping("/auth/logout")
    public void logout(@Valid @RequestBody LogoutRequest req) {
        refreshTokenService.revoke(req.refreshToken());
    }

    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jat)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String userId = jat.getToken().getSubject();
        String email = jat.getToken().getClaimAsString("email");
        String name = jat.getToken().getClaimAsString("name");
        return new MeResponse(userId, email, name);
    }

    private String userAgent(HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }

    private String clientIp(HttpServletRequest req) {
        // good enough for local dev; later you can honor X-Forwarded-For
        return req.getRemoteAddr();
    }
}
