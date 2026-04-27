package com.coachapp.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CoachRegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank String name,
        @NotBlank @Pattern(
                regexp = "^[a-z0-9-]+$",
                message = "Subdomain may only contain lowercase letters, numbers, and hyphens"
        ) String subdomain
) {
}
