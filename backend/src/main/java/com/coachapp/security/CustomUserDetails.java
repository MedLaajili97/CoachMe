package com.coachapp.security;

import com.coachapp.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final User.Role role;
    private final String tenantId;

    /** Built from a DB-loaded User during authentication. */
    public CustomUserDetails(User user, String tenantId) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.role = user.getRole();
        this.tenantId = tenantId;
    }

    /** Built from JWT claims on every authenticated request (no DB hit). */
    public CustomUserDetails(UUID id, String email, User.Role role, String tenantId) {
        this.id = id;
        this.email = email;
        this.password = null;
        this.role = role;
        this.tenantId = tenantId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
