package com.coachapp.exception;

public class TenantAlreadyExistsException extends RuntimeException {
    public TenantAlreadyExistsException(String subdomain) {
        super("Tenant already exists: " + subdomain);
    }
}
