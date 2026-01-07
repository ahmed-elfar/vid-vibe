package com.xay.videos_recommender.cache;

import com.xay.videos_recommender.model.domain.CachedFeed;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FeedCacheManagerImpl implements FeedCacheManager {

    private static final String FEED_CACHE_NAME = "feeds";

    private final CacheManager cacheManager;

    public FeedCacheManagerImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Optional<CachedFeed> getFeed(Long tenantId, String userId) {
        Cache cache = cacheManager.getCache(FEED_CACHE_NAME);
        if (cache == null) {
            return Optional.empty();
        }
        String key = CacheKeyBuilder.userFeed(tenantId, userId);
        CachedFeed feed = cache.get(key, CachedFeed.class);
        return Optional.ofNullable(feed);
    }

    @Override
    public void putFeed(Long tenantId, String userId, CachedFeed feed) {
        Cache cache = cacheManager.getCache(FEED_CACHE_NAME);
        if (cache != null) {
            String key = CacheKeyBuilder.userFeed(tenantId, userId);
            cache.put(key, feed);
        }
    }

    @Override
    public void evictFeed(Long tenantId, String userId) {
        Cache cache = cacheManager.getCache(FEED_CACHE_NAME);
        if (cache != null) {
            String key = CacheKeyBuilder.userFeed(tenantId, userId);
            cache.evict(key);
        }
    }

    @Override
    public void evictAllFeedsForTenant(Long tenantId) {
        Cache cache = cacheManager.getCache(FEED_CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }
}

