package com.coachapp.service;

import com.coachapp.dto.auth.AuthResponse;
import com.coachapp.dto.auth.CoachRegisterRequest;
import com.coachapp.dto.auth.LoginRequest;
import com.coachapp.entity.RefreshToken;
import com.coachapp.entity.Tenant;
import com.coachapp.entity.User;
import com.coachapp.exception.TenantAlreadyExistsException;
import com.coachapp.exception.UserAlreadyExistsException;
import com.coachapp.repository.RefreshTokenRepository;
import com.coachapp.repository.TenantRepository;
import com.coachapp.repository.UserRepository;
import com.coachapp.security.CustomUserDetails;
import com.coachapp.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long REFRESH_TOKEN_VALIDITY_SECONDS = 7L * 24 * 60 * 60;
    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TenantProvisioningService tenantProvisioningService;

    @Transactional
    public AuthResponse registerCoach(CoachRegisterRequest request, HttpServletResponse response) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException(request.email());
        }
        if (tenantRepository.existsBySubdomain(request.subdomain())) {
            throw new TenantAlreadyExistsException(request.subdomain());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(User.Role.COACH)
                .build();
        userRepository.save(user);

        Tenant tenant = Tenant.builder()
                .subdomain(request.subdomain())
                .coachId(user.getId())
                .name(request.name())
                .build();
        tenantRepository.save(tenant);

        // provision runs after save; full impl in #11 — must execute after this transaction commits
        tenantProvisioningService.provision(tenant.getId().toString());

        CustomUserDetails userDetails = new CustomUserDetails(user, request.subdomain());
        return issueTokens(userDetails, response);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        String tenantId = resolveTenantId(principal.getId(), principal.getRole());
        CustomUserDetails withTenant = new CustomUserDetails(
                principal.getId(), principal.getUsername(), principal.getRole(), tenantId);

        return issueTokens(withTenant, response);
    }

    @Transactional
    public AuthResponse refresh(String rawToken, HttpServletResponse response) {
        String hash = hashToken(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        refreshTokenRepository.delete(stored);

        String tenantId = resolveTenantId(user.getId(), user.getRole());
        CustomUserDetails userDetails = new CustomUserDetails(user, tenantId);
        return issueTokens(userDetails, response);
    }

    @Transactional
    public void logout(String rawToken, HttpServletResponse response) {
        if (rawToken != null) {
            String hash = hashToken(rawToken);
            refreshTokenRepository.findByTokenHash(hash)
                    .ifPresent(t -> refreshTokenRepository.deleteByUserId(t.getUserId()));
        }
        clearRefreshCookie(response);
    }

    // --- private helpers ---

    private AuthResponse issueTokens(CustomUserDetails userDetails, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(userDetails, userDetails.getTenantId());

        String rawRefresh = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userDetails.getId())
                .tokenHash(hashToken(rawRefresh))
                .expiresAt(Instant.now().plusSeconds(REFRESH_TOKEN_VALIDITY_SECONDS))
                .build());

        setRefreshCookie(response, rawRefresh);

        return new AuthResponse(
                accessToken,
                new AuthResponse.UserInfo(
                        userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getRole().name(),
                        userDetails.getTenantId()
                )
        );
    }

    private String resolveTenantId(UUID userId, User.Role role) {
        if (role == User.Role.COACH) {
            return tenantRepository.findByCoachId(userId)
                    .map(Tenant::getSubdomain)
                    .orElseThrow(() -> new IllegalStateException("No tenant found for coach: " + userId));
        }
        // CLIENT tenantId resolution is handled in the invite flow — see #14
        throw new IllegalStateException("Client login not supported via this endpoint");
    }

    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(rawToken, REFRESH_TOKEN_VALIDITY_SECONDS));
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0));
    }

    private String buildCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth/refresh")
                .maxAge(maxAgeSeconds)
                .sameSite("Strict")
                .build()
                .toString();
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
