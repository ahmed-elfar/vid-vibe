package com.xay.videos_recommender.service;

import com.xay.videos_recommender.cache.AppCache;
import com.xay.videos_recommender.exception.TenantNotFoundException;
import com.xay.videos_recommender.model.entity.Tenant;
import com.xay.videos_recommender.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AppCache appCache;

    public Tenant getTenant(Long tenantId) {
        return appCache.getTenant(tenantId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new TenantNotFoundException(tenantId));
                    appCache.putTenant(tenantId, tenant);
                    return tenant;
                });
    }

    public List<Tenant> getAllActiveTenants() {
        return tenantRepository.findAll();
    }

    public void updateConfigVersion(Long tenantId) {
        appCache.evictTenant(tenantId);
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setConfigVersion(tenant.getConfigVersion() + 1);
            tenantRepository.save(tenant);
        });
    }

    // Feature flag methods

    public boolean isPersonalizationEnabled(Long tenantId) {
        Tenant tenant = getTenant(tenantId);
        return Boolean.TRUE.equals(tenant.getPersonalizationEnabled());
    }

    public boolean isUserInRollout(Long tenantId, String userId) {
        Tenant tenant = getTenant(tenantId);
        int rolloutPercentage = tenant.getRolloutPercentage() != null ? tenant.getRolloutPercentage() : 100;

        if (rolloutPercentage >= 100) {
            return true;
        }
        if (rolloutPercentage <= 0) {
            return false;
        }

        // Deterministic rollout based on user ID hash
        int userBucket = Math.abs(userId.hashCode() % 100);
        return userBucket < rolloutPercentage;
    }

    public int getRolloutPercentage(Long tenantId) {
        Tenant tenant = getTenant(tenantId);
        return tenant.getRolloutPercentage() != null ? tenant.getRolloutPercentage() : 100;
    }
}
