package com.coachapp.repository;

import com.coachapp.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByTokenHash(String tokenHash);
    boolean existsByTenantIdAndEmailAndStatus(UUID tenantId, String email, Invitation.Status status);
}
