package com.coachapp.controller;

import com.coachapp.dto.ApiResponse;
import com.coachapp.dto.invite.InviteRequest;
import com.coachapp.dto.invite.InviteResponse;
import com.coachapp.security.CustomUserDetails;
import com.coachapp.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coach")
@RequiredArgsConstructor
public class CoachController {

    private final InvitationService invitationService;

    @PostMapping("/clients/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InviteResponse> inviteClient(
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(
                invitationService.createInvite(principal.getTenantId(), request.email()));
    }
}
