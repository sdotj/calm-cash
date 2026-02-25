package com.samjenkins.auth_service.controller;

import com.samjenkins.auth_service.dto.AuthDtos.*;
import com.samjenkins.auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        return authService.register(req, clientIp(http), userAgent(http));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return authService.login(req, clientIp(http), userAgent(http));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        return authService.refresh(req, clientIp(http), userAgent(http));
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req);
    }

    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jat)) {
            throw authService.unauthorized("Unauthorized");
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
