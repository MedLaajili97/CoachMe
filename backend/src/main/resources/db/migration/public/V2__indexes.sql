-- users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- tenants
CREATE INDEX idx_tenants_subdomain ON tenants(subdomain);
CREATE INDEX idx_tenants_coach_id  ON tenants(coach_id);

-- invitations
CREATE INDEX idx_invitations_token_hash ON invitations(token_hash);
CREATE INDEX idx_invitations_tenant_id  ON invitations(tenant_id);
CREATE INDEX idx_invitations_email      ON invitations(email);
CREATE INDEX idx_invitations_status     ON invitations(status);

-- refresh_tokens
CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);