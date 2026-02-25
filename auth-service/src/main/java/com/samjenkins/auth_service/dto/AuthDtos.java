package com.samjenkins.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
        @Email @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotBlank @Size(max = 100) String displayName
    ) {}

    public record LoginRequest(
        @Email @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 128) String password
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
