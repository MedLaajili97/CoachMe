package com.coachapp.dto.invite;

import java.util.UUID;

public record VerifyInviteResponse(UUID invitationId, String email, String tenantSubdomain) {}
