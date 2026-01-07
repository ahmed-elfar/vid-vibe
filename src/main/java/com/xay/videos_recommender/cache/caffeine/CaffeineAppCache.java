package com.xay.videos_recommender.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xay.videos_recommender.cache.AppCache;
import com.xay.videos_recommender.model.domain.ContentCandidate;
import com.xay.videos_recommender.model.entity.Tenant;
import com.xay.videos_recommender.model.entity.UserProfile;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaffeineAppCache implements AppCache {

    private final MeterRegistry meterRegistry;

    @Value("${app.cache.tenant.max-size}")
    private int tenantMaxSize;

    @Value("${app.cache.user-profile.max-size}")
    private int userProfileMaxSize;

    @Value("${app.cache.user-profile.expire-after-write-minutes}")
    private int userProfileExpireMinutes;

    @Value("${app.cache.content-candidates.max-size}")
    private int contentCandidatesMaxSize;

    private Cache<Long, Tenant> tenantCache;
    private Cache<String, UserProfile> userProfileCache;
    private Cache<Long, List<ContentCandidate>> contentCandidatesCache;

    @PostConstruct
    public void init() {
        tenantCache = Caffeine.newBuilder()
                .maximumSize(tenantMaxSize)
                .recordStats()
                .build();

        userProfileCache = Caffeine.newBuilder()
                .maximumSize(userProfileMaxSize)
                .expireAfterWrite(userProfileExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();

        contentCandidatesCache = Caffeine.newBuilder()
                .maximumSize(contentCandidatesMaxSize)
                .recordStats()
                .build();

        // Register caches with Micrometer for metrics
        CaffeineCacheMetrics.monitor(meterRegistry, tenantCache, "tenant", Collections.emptyList());
        CaffeineCacheMetrics.monitor(meterRegistry, userProfileCache, "userProfile", Collections.emptyList());
        CaffeineCacheMetrics.monitor(meterRegistry, contentCandidatesCache, "contentCandidates", Collections.emptyList());

        log.info("CaffeineAppCache initialized: tenantMaxSize={}, userProfileMaxSize={}, " +
                        "userProfileExpireMinutes={}, contentCandidatesMaxSize={}",
                tenantMaxSize, userProfileMaxSize, userProfileExpireMinutes, contentCandidatesMaxSize);
    }

    @Override
    public Optional<Tenant> getTenant(Long tenantId) {
        return Optional.ofNullable(tenantCache.getIfPresent(tenantId));
    }

    @Override
    public void putTenant(Long tenantId, Tenant tenant) {
        tenantCache.put(tenantId, tenant);
    }

    @Override
    public void evictTenant(Long tenantId) {
        tenantCache.invalidate(tenantId);
    }

    @Override
    public Optional<UserProfile> getUserProfile(Long tenantId, String userId) {
        String key = buildUserProfileKey(tenantId, userId);
        return Optional.ofNullable(userProfileCache.getIfPresent(key));
    }

    @Override
    public void putUserProfile(Long tenantId, String userId, UserProfile profile) {
        String key = buildUserProfileKey(tenantId, userId);
        userProfileCache.put(key, profile);
    }

    @Override
    public void evictUserProfile(Long tenantId, String userId) {
        String key = buildUserProfileKey(tenantId, userId);
        userProfileCache.invalidate(key);
    }

    @Override
    public Optional<List<ContentCandidate>> getContentCandidates(Long tenantId) {
        return Optional.ofNullable(contentCandidatesCache.getIfPresent(tenantId));
    }

    @Override
    public void putContentCandidates(Long tenantId, List<ContentCandidate> candidates) {
        contentCandidatesCache.put(tenantId, candidates);
    }

    @Override
    public void evictContentCandidates(Long tenantId) {
        contentCandidatesCache.invalidate(tenantId);
    }

    private String buildUserProfileKey(Long tenantId, String userId) {
        return tenantId + ":" + userId;
    }
}

