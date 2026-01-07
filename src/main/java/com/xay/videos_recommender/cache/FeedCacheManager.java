package com.xay.videos_recommender.cache;

import com.xay.videos_recommender.model.domain.CachedFeed;

import java.util.Optional;

public interface FeedCacheManager {

    Optional<CachedFeed> getFeed(Long tenantId, String userId);

    void putFeed(Long tenantId, String userId, CachedFeed feed);

    void evictFeed(Long tenantId, String userId);

    void evictAllFeedsForTenant(Long tenantId);
}

