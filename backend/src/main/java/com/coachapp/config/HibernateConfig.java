package com.coachapp.config;

import com.coachapp.tenant.TenantConnectionProvider;
import com.coachapp.tenant.TenantIdentifierResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HibernateConfig {

    private final TenantConnectionProvider connectionProvider;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    /**
     * Wires multi-tenancy beans into Hibernate without replacing Spring Boot's
     * auto-configured EntityManagerFactory. MultiTenancyStrategy was removed in
     * Hibernate 6.2 — registering a MultiTenantConnectionProvider is sufficient.
     */
    @Bean
    public HibernatePropertiesCustomizer multiTenancyCustomizer() {
        return properties -> {
            properties.put("hibernate.multi_tenancy_connection_provider", connectionProvider);
            properties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);
        };
    }
}
