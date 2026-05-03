package com.coachapp.config;

import com.coachapp.tenant.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register/coach",
                        "/api/v1/auth/verify-invite",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout"
                );
    }
}
