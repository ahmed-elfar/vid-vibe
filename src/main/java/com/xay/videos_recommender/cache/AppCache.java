package com.xay.videos_recommender.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xay.videos_recommender.model.domain.ContentCandidate;
import com.xay.videos_recommender.model.entity.Tenant;
import com.xay.videos_recommender.model.entity.UserProfile;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AppCache {

    private Cache<Long, Tenant> tenantCache;
    private Cache<String, UserProfile> userProfileCache;
    private Cache<Long, List<ContentCandidate>> contentCandidatesCache;

    @PostConstruct
    public void init() {
        tenantCache = Caffeine.newBuilder()
                .maximumSize(120)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();

        userProfileCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();

        contentCandidatesCache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .recordStats()
                .build();

        log.info("AppCache initialized");
    }

    // Tenant cache operations
    public Optional<Tenant> getTenant(Long tenantId) {
        return Optional.ofNullable(tenantCache.getIfPresent(tenantId));
    }

    public void putTenant(Long tenantId, Tenant tenant) {
        tenantCache.put(tenantId, tenant);
    }

    public void evictTenant(Long tenantId) {
        tenantCache.invalidate(tenantId);
    }

    // User profile cache operations
    public Optional<UserProfile> getUserProfile(Long tenantId, String userId) {
        String key = buildUserProfileKey(tenantId, userId);
        return Optional.ofNullable(userProfileCache.getIfPresent(key));
    }

    public void putUserProfile(Long tenantId, String userId, UserProfile profile) {
        String key = buildUserProfileKey(tenantId, userId);
        userProfileCache.put(key, profile);
    }

    public void evictUserProfile(Long tenantId, String userId) {
        String key = buildUserProfileKey(tenantId, userId);
        userProfileCache.invalidate(key);
    }

    // Content candidates cache operations
    public Optional<List<ContentCandidate>> getContentCandidates(Long tenantId) {
        return Optional.ofNullable(contentCandidatesCache.getIfPresent(tenantId));
    }

    public void putContentCandidates(Long tenantId, List<ContentCandidate> candidates) {
        contentCandidatesCache.put(tenantId, candidates);
    }

    public void evictContentCandidates(Long tenantId) {
        contentCandidatesCache.invalidate(tenantId);
    }

    private String buildUserProfileKey(Long tenantId, String userId) {
        return tenantId + ":" + userId;
    }
}

