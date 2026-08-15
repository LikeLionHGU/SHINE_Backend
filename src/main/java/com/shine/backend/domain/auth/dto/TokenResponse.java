package com.shine.backend.domain.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse of(String access, String refresh, long validityMs) {
        return new TokenResponse(access, refresh, "Bearer", validityMs / 1000);
    }
}
