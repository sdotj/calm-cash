package com.samjenkins.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String displayName
    ) {}

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(@NotBlank String refreshToken) {}

    public record AuthResponse(
        String accessToken,
        String refreshToken
    ) {}

    public record MeResponse(
        String userId,
        String email,
        String displayName
    ) {}
}
