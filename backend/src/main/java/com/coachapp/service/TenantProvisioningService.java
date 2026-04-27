package com.coachapp.service;

import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final DataSource dataSource;

    /**
     * Creates schema tenant_{tenantId} and applies all tenant Flyway migrations.
     * Flyway opens its own JDBC connection — does not participate in the caller's transaction.
     * Idempotent: safe to call again if a previous attempt failed mid-migration.
     */
    public void provision(String tenantId) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("tenant_" + tenantId)
                .locations("classpath:db/migration/tenant")
                .load()
                .migrate();
    }
}
