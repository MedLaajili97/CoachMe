package com.coachapp.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final Pattern SAFE_TENANT_ID = Pattern.compile("^[a-z0-9-]+$");

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId == null || !SAFE_TENANT_ID.matcher(tenantId).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing or invalid X-Tenant-ID header");
            return false;
        }
        TenantContext.set(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        TenantContext.clear();
    }
}
