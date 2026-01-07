package com.xay.videos_recommender.service;

import com.xay.videos_recommender.cache.FeedCacheManager;
import com.xay.videos_recommender.model.domain.CachedFeed;
import com.xay.videos_recommender.model.domain.ContentCandidate;
import com.xay.videos_recommender.model.domain.RankedVideo;
import com.xay.videos_recommender.model.domain.UserSignals;
import com.xay.videos_recommender.model.dto.response.FeedItem;
import com.xay.videos_recommender.model.dto.response.FeedResponse;
import com.xay.videos_recommender.model.entity.Tenant;
import com.xay.videos_recommender.model.entity.Video;
import com.xay.videos_recommender.repository.VideoRepository;
import com.xay.videos_recommender.util.CursorUtil;
import com.xay.videos_recommender.util.ETagUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private ContentService contentService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private RankingService rankingService;

    @Mock
    private FeedCacheManager feedCacheManager;

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private FeedService feedService;

    private static final Long TENANT_ID = 1L;
    private static final String USER_ID = "user_abc123";
    private static final int DEFAULT_LIMIT = 5;

    private List<ContentCandidate> sampleCandidates;
    private List<RankedVideo> sampleRankedVideos;
    private List<Video> sampleVideos;
    private UserSignals activeUserSignals;
    private UserSignals coldStartUserSignals;
    private Tenant sampleTenant;

    @BeforeEach
    void setUp() {
        // Sample content candidates (10 videos)
        sampleCandidates = List.of(
                createCandidate(1L, "vid001", "sports"),
                createCandidate(2L, "vid002", "news"),
                createCandidate(3L, "vid003", "entertainment"),
                createCandidate(4L, "vid004", "sports"),
                createCandidate(5L, "vid005", "music"),
                createCandidate(6L, "vid006", "sports"),
                createCandidate(7L, "vid007", "news"),
                createCandidate(8L, "vid008", "entertainment"),
                createCandidate(9L, "vid009", "music"),
                createCandidate(10L, "vid010", "sports")
        );

        // Sample ranked videos
        sampleRankedVideos = sampleCandidates.stream()
                .map(c -> RankedVideo.builder()
                        .videoId(c.videoId())
                        .externalId(c.externalId())
                        .score(100.0 - c.videoId())
                        .reason("personalized")
                        .build())
                .toList();

        // Sample video entities
        sampleVideos = sampleCandidates.stream()
                .map(c -> Video.builder()
                        .id(c.videoId())
                        .externalId(c.externalId())
                        .title("Video " + c.videoId())
                        .category(c.category())
                        .durationSeconds(120)
                        .build())
                .toList();

        // Active user with watch history
        activeUserSignals = UserSignals.builder()
                .tenantId(TENANT_ID)
                .hashedUserId(USER_ID)
                .watchCount(50)
                .totalWatchTimeMs(3600000L)
                .avgWatchPercentage(BigDecimal.valueOf(0.75))
                .likeCount(10)
                .shareCount(2)
                .categoryAffinities(Map.of("sports", 0.8, "news", 0.5))
                .lastWatchedIds(List.of("vid001", "vid002"))
                .build();

        // Cold-start user (no history)
        coldStartUserSignals = UserSignals.builder()
                .tenantId(TENANT_ID)
                .hashedUserId(USER_ID)
                .watchCount(0)
                .totalWatchTimeMs(0L)
                .avgWatchPercentage(BigDecimal.ZERO)
                .likeCount(0)
                .shareCount(0)
                .categoryAffinities(Map.of())
                .lastWatchedIds(List.of())
                .build();

        // Sample tenant
        sampleTenant = Tenant.builder()
                .id(TENANT_ID)
                .name("Test Tenant")
                .personalizationEnabled(true)
                .rolloutPercentage(100)
                .build();
    }

    private ContentCandidate createCandidate(Long id, String externalId, String category) {
        return ContentCandidate.builder()
                .videoId(id)
                .externalId(externalId)
                .category(category)
                .tags(List.of())
                .baseScore(BigDecimal.valueOf(50))
                .editorialBoost(BigDecimal.ONE)
                .freshnessScore(BigDecimal.valueOf(0.9))
                .engagementScore(BigDecimal.valueOf(0.7))
                .maturityRating("PG")
                .build();
    }

    @Nested
    @DisplayName("ETag and 304 Not Modified scenarios")
    class ETagScenarios {

        @Test
        @DisplayName("First page request without ETag returns personalized feed")
        void firstPageWithoutETag_returnsPersonalizedFeed() {
            // Given
            setupPersonalizationEnabled();
            setupNoCachedFeed();
            setupActiveUser();
            setupContentAndRanking();

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, null, null);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().items()).hasSize(DEFAULT_LIMIT);
            assertThat(response.get().meta().feedType()).isEqualTo("personalized");
            assertThat(response.get().pagination().hasMore()).isTrue();
            assertThat(response.get().pagination().nextCursor()).isNotNull();
        }

        @Test
        @DisplayName("First page with matching ETag returns null (304 Not Modified)")
        void firstPageWithMatchingETag_returnsNull() {
            // Given
            int candidatesVersion = 2;
            int feedVersion = 3;
            int firstPageCursor = 0;
            String matchingETag = ETagUtil.generate(candidatesVersion, feedVersion, firstPageCursor);

            setupPersonalizationEnabled();
            when(feedCacheManager.getFeed(TENANT_ID, USER_ID))
                    .thenReturn(Optional.of(createCachedFeed(feedVersion)));
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(candidatesVersion);

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, null, matchingETag);

            // Then
            assertThat(response).isEmpty(); // Controller handles 304
        }

        @Test
        @DisplayName("First page with non-matching ETag returns feed with data")
        void firstPageWithNonMatchingETag_returnsFeed() {
            // Given
            int candidatesVersion = 2;
            int feedVersion = 3;
            String oldETag = "1x2"; // Different from current

            setupPersonalizationEnabled();
            when(feedCacheManager.getFeed(TENANT_ID, USER_ID))
                    .thenReturn(Optional.of(createCachedFeed(feedVersion)));
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(candidatesVersion);

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, null, oldETag);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().items()).hasSize(DEFAULT_LIMIT);
        }

        @Test
        @DisplayName("Paginated request with page 1 ETag returns data (different cursor = different ETag)")
        void paginatedRequestWithPage1ETag_returnsData() {
            // Given
            int candidatesVersion = 2;
            int feedVersion = 3;
            int page1Cursor = 0;
            int page2Cursor = 5;
            String page1ETag = ETagUtil.generate(candidatesVersion, feedVersion, page1Cursor);
            String cursor = CursorUtil.encode(page2Cursor); // Page 2

            setupPersonalizationEnabled();
            when(feedCacheManager.getFeed(TENANT_ID, USER_ID))
                    .thenReturn(Optional.of(createCachedFeed(feedVersion)));
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(candidatesVersion);

            // When - request page 2 with page 1's ETag
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, cursor, page1ETag);

            // Then - should return data because ETags don't match (different cursors)
            assertThat(response).isPresent();
            assertThat(response.get().items()).hasSize(DEFAULT_LIMIT); // Videos 6-10
        }

        @Test
        @DisplayName("Same page request with matching ETag returns 304")
        void samePageWithMatchingETag_returns304() {
            // Given
            int candidatesVersion = 2;
            int feedVersion = 3;
            int cursor = 5;
            String matchingETag = ETagUtil.generate(candidatesVersion, feedVersion, cursor);
            String cursorString = CursorUtil.encode(cursor);

            setupPersonalizationEnabled();
            when(feedCacheManager.getFeed(TENANT_ID, USER_ID))
                    .thenReturn(Optional.of(createCachedFeed(feedVersion)));
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(candidatesVersion);

            // When - request same page with matching ETag
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, cursorString, matchingETag);

            // Then - should return empty (304) because ETag matches for same cursor
            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("Fallback scenarios")
    class FallbackScenarios {

        @Test
        @DisplayName("Cold-start user receives cold_start fallback feed")
        void coldStartUser_returnsFallbackFeed() {
            // Given
            setupPersonalizationEnabled();
            setupNoCachedFeed();
            when(userProfileService.getUserSignals(TENANT_ID, USER_ID))
                    .thenReturn(coldStartUserSignals);
            when(contentService.getContentCandidates(TENANT_ID))
                    .thenReturn(sampleCandidates);
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(1);
            when(rankingService.rankWithoutPersonalization(anyList(), anyInt()))
                    .thenReturn(sampleRankedVideos);
            when(videoRepository.findAllById(anyList()))
                    .thenReturn(sampleVideos);

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, null, null);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().meta().feedType()).isEqualTo("cold_start");
        }

        @Test
        @DisplayName("Personalization disabled returns fallback feed")
        void personalizationDisabled_returnsFallbackFeed() {
            // Given
            when(tenantService.isPersonalizationEnabled(TENANT_ID)).thenReturn(false);
            when(contentService.getContentCandidates(TENANT_ID))
                    .thenReturn(sampleCandidates);
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(1);
            when(rankingService.rankWithoutPersonalization(anyList(), anyInt()))
                    .thenReturn(sampleRankedVideos);
            when(videoRepository.findAllById(anyList()))
                    .thenReturn(sampleVideos);

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, null, null);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().meta().feedType()).isEqualTo("fallback");
        }

        @Test
        @DisplayName("User not in rollout returns fallback feed")
        void userNotInRollout_returnsFallbackFeed() {
            // Given
            when(tenantService.isPersonalizationEnabled(TENANT_ID)).thenReturn(true);
            when(tenantService.isUserInRollout(TENANT_ID, USER_ID)).thenReturn(false);
            when(contentService.getContentCandidates(TENANT_ID))
                    .thenReturn(sampleCandidates);
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(1);
            when(rankingService.rankWithoutPersonalization(anyList(), anyInt()))
                    .thenReturn(sampleRankedVideos);
            when(videoRepository.findAllById(anyList()))
                    .thenReturn(sampleVideos);

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, null, null);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().meta().feedType()).isEqualTo("fallback");
        }

        @Test
        @DisplayName("Empty content returns empty feed")
        void emptyContent_returnsEmptyFeed() {
            // Given
            setupPersonalizationEnabled();
            setupNoCachedFeed();
            setupActiveUser();
            when(contentService.getContentCandidates(TENANT_ID))
                    .thenReturn(List.of()); // No content
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(1);

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, null, null);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().items()).isEmpty();
            assertThat(response.get().meta().feedType()).isEqualTo("no_content");
        }
    }

    @Nested
    @DisplayName("Pagination scenarios")
    class PaginationScenarios {

        @Test
        @DisplayName("First page has correct pagination info")
        void firstPage_hasCorrectPagination() {
            // Given
            setupPersonalizationEnabled();
            setupNoCachedFeed();
            setupActiveUser();
            setupContentAndRanking();

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, null, null);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().items()).hasSize(5);
            assertThat(response.get().pagination().hasMore()).isTrue();
            assertThat(response.get().pagination().nextCursor()).isNotNull();

            // Decode cursor to verify it points to offset 5
            int nextOffset = CursorUtil.decode(response.get().pagination().nextCursor());
            assertThat(nextOffset).isEqualTo(5);
        }

        @Test
        @DisplayName("Second page returns correct items")
        void secondPage_returnsCorrectItems() {
            // Given
            int feedVersion = 1;
            String cursor = CursorUtil.encode(5);

            setupPersonalizationEnabled();
            when(feedCacheManager.getFeed(TENANT_ID, USER_ID))
                    .thenReturn(Optional.of(createCachedFeed(feedVersion)));
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(1);

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, cursor, null);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().items()).hasSize(5); // Videos 6-10
            assertThat(response.get().pagination().hasMore()).isFalse(); // Last page
        }

        @Test
        @DisplayName("Limit larger than total items returns all items")
        void largeLimitReturnsAllItems() {
            // Given
            setupPersonalizationEnabled();
            setupNoCachedFeed();
            setupActiveUser();
            setupContentAndRanking();

            // When (limit=100, but only 10 videos)
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, 100, null, null);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().items()).hasSize(10);
            assertThat(response.get().pagination().hasMore()).isFalse();
            assertThat(response.get().pagination().nextCursor()).isNull();
        }

        @Test
        @DisplayName("Cursor beyond items returns empty page")
        void cursorBeyondItems_returnsEmptyPage() {
            // Given
            int feedVersion = 1;
            String cursor = CursorUtil.encode(100); // Way beyond available items

            setupPersonalizationEnabled();
            when(feedCacheManager.getFeed(TENANT_ID, USER_ID))
                    .thenReturn(Optional.of(createCachedFeed(feedVersion)));
            when(contentService.getContentCandidatesVersion(TENANT_ID))
                    .thenReturn(1);

            // When
            Optional<FeedResponse> response = feedService.generateFeed(TENANT_ID, USER_ID, DEFAULT_LIMIT, cursor, null);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().items()).isEmpty();
            assertThat(response.get().pagination().hasMore()).isFalse();
        }
    }

    // Helper methods

    private void setupPersonalizationEnabled() {
        when(tenantService.isPersonalizationEnabled(TENANT_ID)).thenReturn(true);
        when(tenantService.isUserInRollout(TENANT_ID, USER_ID)).thenReturn(true);
    }

    private void setupNoCachedFeed() {
        when(feedCacheManager.getFeed(TENANT_ID, USER_ID)).thenReturn(Optional.empty());
        when(contentService.getContentCandidatesVersion(TENANT_ID)).thenReturn(1);
    }

    private void setupActiveUser() {
        when(userProfileService.getUserSignals(TENANT_ID, USER_ID))
                .thenReturn(activeUserSignals);
    }

    private void setupContentAndRanking() {
        when(contentService.getContentCandidates(TENANT_ID))
                .thenReturn(sampleCandidates);
        when(tenantService.getTenant(TENANT_ID))
                .thenReturn(sampleTenant);
        when(rankingService.rank(anyList(), any(), any(), anyInt()))
                .thenReturn(sampleRankedVideos);
        when(videoRepository.findAllById(anyList()))
                .thenReturn(sampleVideos);
    }

    private CachedFeed createCachedFeed(int version) {
        List<FeedItem> items = sampleVideos.stream()
                .map(v -> new FeedItem(
                        String.valueOf(v.getId()),
                        v.getExternalId(),
                        v.getTitle(),
                        "https://cdn.example.com/thumb/" + v.getExternalId() + ".jpg",
                        v.getDurationSeconds(),
                        v.getCategory()
                ))
                .toList();

        return CachedFeed.builder()
                .version(version)
                .generatedAt(Instant.now())
                .feedType("personalized")
                .items(items)
                .build();
    }
}

