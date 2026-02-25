package com.samjenkins.auth_service.util;

public class ServiceConstants {
    private ServiceConstants() {}

    //Common error messages
    public static final String INVALID_TOKEN = "Invalid token";
    public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token expired or revoked";
    public static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String EMAIL_IN_USE = "Email already in use";
    public static final String TOO_MANY_LOGIN_ATTEMPTS = "Too many login attempts";
}
