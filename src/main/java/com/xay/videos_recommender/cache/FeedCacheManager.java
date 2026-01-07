package com.xay.videos_recommender.cache;

import com.xay.videos_recommender.model.domain.CachedFeed;

import java.util.Optional;

/**
 * Cache manager interface for user feeds.
 * In production, this would be backed by Redis.
 */
public interface FeedCacheManager {

    Optional<CachedFeed> getFeed(Long tenantId, String userId);

    void putFeed(Long tenantId, String userId, CachedFeed feed);

    void invalidateFeed(Long tenantId, String userId);

    void invalidateAllForTenant(Long tenantId);
}
