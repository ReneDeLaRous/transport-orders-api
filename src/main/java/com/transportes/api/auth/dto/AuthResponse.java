package com.transportes.api.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMinutes) {
}
