package com.coachapp.service;

import com.coachapp.dto.invite.InviteResponse;
import com.coachapp.dto.invite.VerifyInviteResponse;
import com.coachapp.entity.Invitation;
import com.coachapp.entity.Tenant;
import com.coachapp.exception.InvitationAlreadyPendingException;
import com.coachapp.exception.InvitationAlreadyUsedException;
import com.coachapp.exception.InvitationExpiredException;
import com.coachapp.exception.InvitationNotFoundException;
import com.coachapp.repository.InvitationRepository;
import com.coachapp.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final int INVITE_EXPIRY_DAYS = 7;

    private final InvitationRepository invitationRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;

    @Transactional
    public InviteResponse createInvite(String tenantSubdomain, String email) {
        Tenant tenant = tenantRepository.findBySubdomain(tenantSubdomain)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantSubdomain));

        if (invitationRepository.existsByTenantIdAndEmailAndStatus(
                tenant.getId(), email, Invitation.Status.PENDING)) {
            throw new InvitationAlreadyPendingException(email);
        }

        String rawToken = UUID.randomUUID().toString();
        Invitation invitation = Invitation.builder()
                .tenantId(tenant.getId())
                .email(email)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plus(INVITE_EXPIRY_DAYS, ChronoUnit.DAYS))
                .build();
        invitationRepository.save(invitation);

        emailService.sendInvitation(email, tenantSubdomain, rawToken);

        return new InviteResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getStatus().name(),
                invitation.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public VerifyInviteResponse verifyInvite(String rawToken) {
        Invitation invitation = findValidPendingInvitation(rawToken);
        Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Tenant not found for invitation"));
        return new VerifyInviteResponse(invitation.getId(), invitation.getEmail(), tenant.getSubdomain());
    }

    private Invitation findValidPendingInvitation(String rawToken) {
        Invitation invitation = invitationRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(InvitationNotFoundException::new);
        if (invitation.getStatus() == Invitation.Status.ACCEPTED) {
            throw new InvitationAlreadyUsedException();
        }
        if (invitation.getStatus() != Invitation.Status.PENDING
                || invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new InvitationExpiredException();
        }
        return invitation;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
