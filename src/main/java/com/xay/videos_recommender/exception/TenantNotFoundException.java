package com.xay.videos_recommender.exception;

import lombok.Getter;

@Getter
public class TenantNotFoundException extends RuntimeException {

    private final Long tenantId;

    public TenantNotFoundException(Long tenantId) {
        super("Tenant not found: " + tenantId);
        this.tenantId = tenantId;
    }
}
