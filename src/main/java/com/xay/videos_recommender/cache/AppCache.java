package com.xay.videos_recommender.cache;

import com.xay.videos_recommender.model.domain.ContentCandidate;
import com.xay.videos_recommender.model.entity.Tenant;
import com.xay.videos_recommender.model.entity.UserProfile;

import java.util.List;
import java.util.Optional;

/**
 * Application cache interface for tenant, user profile, and content candidates.
 * In production, this would be backed by Redis.
 */
public interface AppCache {

    // Tenant cache operations
    Optional<Tenant> getTenant(Long tenantId);
    void putTenant(Long tenantId, Tenant tenant);
    void evictTenant(Long tenantId);

    // User profile cache operations
    Optional<UserProfile> getUserProfile(Long tenantId, String userId);
    void putUserProfile(Long tenantId, String userId, UserProfile profile);
    void evictUserProfile(Long tenantId, String userId);

    // Content candidates cache operations
    Optional<List<ContentCandidate>> getContentCandidates(Long tenantId);
    void putContentCandidates(Long tenantId, List<ContentCandidate> candidates);
    void evictContentCandidates(Long tenantId);
}
