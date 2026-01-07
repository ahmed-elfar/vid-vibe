package com.xay.videos_recommender.cache;

import com.xay.videos_recommender.model.domain.CachedFeed;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache manager for user feeds.
 * In production, this would be backed by Redis.
 */
@Component
public class FeedCacheManager {

    // Cache key: "tenantId:userId" -> CachedFeed
    private final Map<String, CachedFeed> feedCache = new ConcurrentHashMap<>();

    public Optional<CachedFeed> getFeed(Long tenantId, String userId) {
        String key = buildKey(tenantId, userId);
        return Optional.ofNullable(feedCache.get(key));
    }

    public void putFeed(Long tenantId, String userId, CachedFeed feed) {
        String key = buildKey(tenantId, userId);
        feedCache.put(key, feed);
    }

    public void invalidateFeed(Long tenantId, String userId) {
        String key = buildKey(tenantId, userId);
        feedCache.remove(key);
    }

    public void invalidateAllForTenant(Long tenantId) {
        String prefix = tenantId + ":";
        feedCache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private String buildKey(Long tenantId, String userId) {
        return tenantId + ":" + userId;
    }
}
