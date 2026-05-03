package com.coachapp.dto.invite;

import java.time.Instant;
import java.util.UUID;

public record InviteResponse(UUID id, String email, String status, Instant expiresAt) {}
