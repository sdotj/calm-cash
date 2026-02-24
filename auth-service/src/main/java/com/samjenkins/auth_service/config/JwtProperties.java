package com.samjenkins.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String issuer,
    String secret,
    int accessTokenMinutes,
    int refreshTokenDays
) {}
