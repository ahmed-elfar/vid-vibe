package com.xay.videos_recommender.service;

import com.xay.videos_recommender.model.entity.Tenant;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TenantService {

    public Optional<Tenant> getTenant(Long tenantId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<Tenant> getAllActiveTenants() {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<Tenant> getTopTenantsByActivity(int limit) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void updateConfigVersion(Long tenantId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

