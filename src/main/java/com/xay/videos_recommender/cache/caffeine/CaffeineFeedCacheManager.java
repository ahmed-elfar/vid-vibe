package com.xay.videos_recommender.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xay.videos_recommender.cache.FeedCacheManager;
import com.xay.videos_recommender.model.domain.CachedFeed;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CaffeineFeedCacheManager implements FeedCacheManager {

    @Value("${app.cache.feed.max-size}")
    private int feedMaxSize;

    @Value("${app.cache.feed.expire-after-write-minutes}")
    private int feedExpireMinutes;

    private Cache<String, CachedFeed> feedCache;

    @PostConstruct
    public void init() {
        feedCache = Caffeine.newBuilder()
                .maximumSize(feedMaxSize)
                .expireAfterWrite(feedExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();

        log.info("CaffeineFeedCacheManager initialized: feedMaxSize={}, feedExpireMinutes={}",
                feedMaxSize, feedExpireMinutes);
    }

    @Override
    public Optional<CachedFeed> getFeed(Long tenantId, String userId) {
        String key = buildKey(tenantId, userId);
        return Optional.ofNullable(feedCache.getIfPresent(key));
    }

    @Override
    public void putFeed(Long tenantId, String userId, CachedFeed feed) {
        String key = buildKey(tenantId, userId);
        feedCache.put(key, feed);
    }

    @Override
    public void invalidateFeed(Long tenantId, String userId) {
        String key = buildKey(tenantId, userId);
        feedCache.invalidate(key);
    }

    @Override
    public void invalidateAllForTenant(Long tenantId) {
        String prefix = tenantId + ":";
        feedCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    private String buildKey(Long tenantId, String userId) {
        return tenantId + ":" + userId;
    }
}

