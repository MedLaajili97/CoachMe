package com.coachapp.dto.auth;

import java.util.UUID;

public record AuthResponse(String accessToken, UserInfo user) {

    public record UserInfo(UUID id, String email, String role, String tenantId) {
    }
}
