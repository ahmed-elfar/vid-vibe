package com.xay.videos_recommender.service;

import com.xay.videos_recommender.cache.FeedCacheManager;
import com.xay.videos_recommender.model.domain.CachedFeed;
import com.xay.videos_recommender.model.domain.ContentCandidate;
import com.xay.videos_recommender.model.domain.RankedVideo;
import com.xay.videos_recommender.model.domain.UserSignals;
import com.xay.videos_recommender.model.dto.response.FeedItem;
import com.xay.videos_recommender.model.dto.response.FeedMeta;
import com.xay.videos_recommender.model.dto.response.FeedResponse;
import com.xay.videos_recommender.model.dto.response.PaginationInfo;
import com.xay.videos_recommender.model.entity.Tenant;
import com.xay.videos_recommender.model.entity.Video;
import com.xay.videos_recommender.repository.VideoRepository;
import com.xay.videos_recommender.util.CursorUtil;
import com.xay.videos_recommender.util.ETagUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private static final int TTL_HINT_SECONDS = 30;
    private static final long TIMEOUT_MS = 600;

    private final TenantService tenantService;
    private final ContentService contentService;
    private final UserProfileService userProfileService;
    private final RankingService rankingService;
    private final FeedCacheManager feedCacheManager;
    private final VideoRepository videoRepository;

    public FeedResponse generateFeed(Long tenantId, String userId, int limit, String cursor, String ifNoneMatch) {
        long startTime = System.currentTimeMillis();
        int offset = CursorUtil.decode(cursor);

        // 1. Check feature flag
        if (!tenantService.isPersonalizationEnabled(tenantId) ||
            !tenantService.isUserInRollout(tenantId, userId)) {
            return generateFallbackFeed(tenantId, limit, offset, "fallback");
        }

        // 2. Check cache
        Optional<CachedFeed> cachedFeed = feedCacheManager.getFeed(tenantId, userId);
        int candidatesVersion = contentService.getContentCandidatesVersion(tenantId);
        
        if (cachedFeed.isPresent()) {
            CachedFeed feed = cachedFeed.get();
            String currentETag = ETagUtil.generate(candidatesVersion, feed.version());
            
            // Check if client's ETag matches (304 Not Modified scenario)
            if (ifNoneMatch != null && ifNoneMatch.equals(currentETag)) {
                // Return empty response - controller will handle 304
                return null;
            }
            
            // Return cached feed with pagination
            return buildResponseFromCachedFeed(feed, limit, offset, currentETag);
        }

        // 3. Get user signals (cold-start check)
        UserSignals userSignals = userProfileService.getUserSignals(tenantId, userId);
        
        if (userSignals.watchCount() == 0) {
            // Cold-start user - return non-personalized feed
            return generateFallbackFeed(tenantId, limit, offset, "cold_start");
        }

        // 4. Get content candidates
        List<ContentCandidate> candidates = contentService.getContentCandidates(tenantId);
        
        if (candidates.isEmpty()) {
            return generateEmptyFeed("no_content");
        }

        // 5. Check timeout
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
            log.warn("Feed generation timeout for tenant {} user {}", tenantId, userId);
            return generateFallbackFeed(tenantId, limit, offset, "timeout_fallback");
        }

        // 6. Rank candidates
        Tenant tenant = tenantService.getTenant(tenantId);
        List<RankedVideo> rankedVideos = rankingService.rank(candidates, userSignals, tenant, candidates.size());

        // 7. Build and cache feed
        List<FeedItem> feedItems = buildFeedItems(tenantId, rankedVideos);
        int feedVersion = candidatesVersion; // Use candidates version as feed version
        
        CachedFeed newFeed = CachedFeed.builder()
                .version(feedVersion)
                .generatedAt(Instant.now())
                .feedType("personalized")
                .items(feedItems)
                .build();
        feedCacheManager.putFeed(tenantId, userId, newFeed);

        // 8. Return paginated response
        String etag = ETagUtil.generate(candidatesVersion, feedVersion);
        return buildPaginatedResponse(feedItems, limit, offset, "personalized", etag);
    }

    private FeedResponse generateFallbackFeed(Long tenantId, int limit, int offset, String feedType) {
        List<ContentCandidate> candidates = contentService.getContentCandidates(tenantId);
        List<RankedVideo> rankedVideos = rankingService.rankWithoutPersonalization(candidates, candidates.size());
        List<FeedItem> feedItems = buildFeedItems(tenantId, rankedVideos);
        
        int version = contentService.getContentCandidatesVersion(tenantId);
        String etag = ETagUtil.generate(version, 0);
        
        return buildPaginatedResponse(feedItems, limit, offset, feedType, etag);
    }

    private FeedResponse generateEmptyFeed(String feedType) {
        return new FeedResponse(
                List.of(),
                new PaginationInfo(null, false),
                new FeedMeta(feedType, Instant.now(), TTL_HINT_SECONDS)
        );
    }

    private FeedResponse buildResponseFromCachedFeed(CachedFeed feed, int limit, int offset, String etag) {
        List<FeedItem> items = feed.items();
        return buildPaginatedResponse(items, limit, offset, feed.feedType(), etag);
    }

    private FeedResponse buildPaginatedResponse(List<FeedItem> allItems, int limit, int offset, String feedType, String etag) {
        int endIndex = Math.min(offset + limit, allItems.size());
        List<FeedItem> pageItems = allItems.subList(Math.min(offset, allItems.size()), endIndex);
        
        boolean hasMore = endIndex < allItems.size();
        String nextCursor = hasMore ? CursorUtil.encode(endIndex) : null;

        return new FeedResponse(
                pageItems,
                new PaginationInfo(nextCursor, hasMore),
                new FeedMeta(feedType, Instant.now(), TTL_HINT_SECONDS)
        );
    }

    private List<FeedItem> buildFeedItems(Long tenantId, List<RankedVideo> rankedVideos) {
        // Fetch video details
        List<Long> videoIds = rankedVideos.stream()
                .map(RankedVideo::videoId)
                .toList();
        
        Map<Long, Video> videoMap = videoRepository.findAllById(videoIds).stream()
                .collect(Collectors.toMap(Video::getId, Function.identity()));

        return rankedVideos.stream()
                .map(ranked -> {
                    Video video = videoMap.get(ranked.videoId());
                    if (video == null) {
                        return null;
                    }
                    return new FeedItem(
                            String.valueOf(video.getId()),
                            video.getExternalId(),
                            video.getTitle(),
                            buildThumbnailUrl(video.getExternalId()),
                            video.getDurationSeconds(),
                            video.getCategory()
                    );
                })
                .filter(item -> item != null)
                .toList();
    }

    private String buildThumbnailUrl(String externalId) {
        return "https://cdn.example.com/thumb/" + externalId + ".jpg";
    }
}
