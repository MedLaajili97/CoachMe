package com.coachapp.service;

import org.springframework.stereotype.Service;

@Service
public class TenantProvisioningService {

    /**
     * Creates tenant schema and applies Flyway migrations for the given tenantId.
     * Implemented in #11 — must run after the public-schema User+Tenant records are committed.
     */
    public void provision(String tenantId) {
        // TODO: implemented in #11
    }
}
