package com.coachapp.dto.invite;

import jakarta.validation.constraints.NotBlank;

public record VerifyInviteRequest(@NotBlank String token) {}
