package com.xay.videos_recommender;

import com.xay.videos_recommender.model.entity.Tenant;
import com.xay.videos_recommender.model.entity.UserProfile;
import com.xay.videos_recommender.model.entity.Video;
import com.xay.videos_recommender.repository.TenantRepository;
import com.xay.videos_recommender.repository.UserProfileRepository;
import com.xay.videos_recommender.repository.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootApplication
public class VideosRecommenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideosRecommenderApplication.class, args);
    }


    /**
     * Seeds sample data for demo purposes.
     * Creates tenants, videos, and user profiles with predefined affinities.
     */
    @Bean
    CommandLineRunner initSampleData(
            TenantRepository tenantRepository,
            VideoRepository videoRepository,
            UserProfileRepository userProfileRepository
    ) {
        return new SampleDataSeeder(tenantRepository, videoRepository, userProfileRepository);
    }

    private record SampleDataSeeder(TenantRepository tenantRepository, VideoRepository videoRepository,
                            UserProfileRepository userProfileRepository) implements CommandLineRunner {

        @Override
            public void run(String... args) {
                log.info("Initializing sample data...");

                // Create tenants
                Tenant tenant1 = tenantRepository.save(createTenant("Acme Corp", true, 100));
                Tenant tenant2 = tenantRepository.save(createTenant("Beta Inc", true, 50));
                Tenant tenant3 = tenantRepository.save(createTenant("Gamma LLC", false, 0));

                log.info("Created tenants: {}, {}, {}", tenant1.getId(), tenant2.getId(), tenant3.getId());

                // Create videos for tenants
                createVideosForTenant(tenant1.getId());
                createVideosForTenant(tenant2.getId());

                // Create user profiles and log their IDs
                List<String> userIds1 = createUserProfiles(tenant1.getId());
                List<String> userIds2 = createUserProfiles(tenant2.getId());

                log.info("Sample data initialization complete!");
                log.info("===========================================");
                log.info("Sample User IDs you can use for testing:");
                log.info("  Tenant {}: {}", tenant1.getId(), userIds1);
                log.info("  Tenant {}: {}", tenant2.getId(), userIds2);
                log.info("===========================================");
            }

            private Tenant createTenant(String name, boolean personalizationEnabled, int rolloutPercentage) {
                return Tenant.builder()
                        .name(name)
                        .personalizationEnabled(personalizationEnabled)
                        .rolloutPercentage(rolloutPercentage)
                        .rankingWeights("{\"recency\": 0.3, \"engagement\": 0.4, \"affinity\": 0.3}")
                        .maturityFilter("PG-13")
                        .configVersion(1)
                        .build();
            }

            private void createVideosForTenant(Long tenantId) {
                // Sports videos
                videoRepository.save(createVideo(tenantId, "sports_001", "Top 10 Goals of the Week", "sports",
                        180, 10000L, 500L, 50L, BigDecimal.valueOf(0.75), BigDecimal.ONE, 0));
                videoRepository.save(createVideo(tenantId, "sports_002", "NBA Highlights: Lakers vs Celtics", "sports",
                        240, 8000L, 400L, 30L, BigDecimal.valueOf(0.70), BigDecimal.ONE, 1));
                videoRepository.save(createVideo(tenantId, "sports_003", "F1 Race Recap: Monaco GP", "sports",
                        300, 15000L, 800L, 100L, BigDecimal.valueOf(0.80), BigDecimal.valueOf(1.5), 2));

                // Comedy videos
                videoRepository.save(createVideo(tenantId, "comedy_001", "Office Prank Gone Wrong", "comedy",
                        95, 50000L, 2000L, 500L, BigDecimal.valueOf(0.85), BigDecimal.ONE, 3));
                videoRepository.save(createVideo(tenantId, "comedy_002", "Stand-up: Weekend Special", "comedy",
                        600, 30000L, 1500L, 200L, BigDecimal.valueOf(0.65), BigDecimal.ONE, 5));
                videoRepository.save(createVideo(tenantId, "comedy_003", "Funny Cat Compilation", "comedy",
                        120, 100000L, 5000L, 1000L, BigDecimal.valueOf(0.90), BigDecimal.valueOf(1.2), 1));

                // News videos
                videoRepository.save(createVideo(tenantId, "news_001", "Tech News Roundup", "news",
                        180, 5000L, 100L, 20L, BigDecimal.valueOf(0.50), BigDecimal.ONE, 0));
                videoRepository.save(createVideo(tenantId, "news_002", "Market Update: Stocks Rally", "news",
                        120, 3000L, 50L, 10L, BigDecimal.valueOf(0.45), BigDecimal.ONE, 2));

                // Music videos
                videoRepository.save(createVideo(tenantId, "music_001", "Summer Hits 2025", "music",
                        210, 200000L, 10000L, 3000L, BigDecimal.valueOf(0.95), BigDecimal.valueOf(2.0), 0));
                videoRepository.save(createVideo(tenantId, "music_002", "Acoustic Sessions: Indie Vibes", "music",
                        300, 20000L, 1000L, 150L, BigDecimal.valueOf(0.80), BigDecimal.ONE, 4));

                log.info("Created 10 videos for tenant {}", tenantId);
            }

            private Video createVideo(Long tenantId, String externalId, String title, String category,
                                      int durationSeconds, long views, long likes, long shares,
                                      BigDecimal avgWatch, BigDecimal editorialBoost, int daysAgo) {
                return Video.builder()
                        .tenantId(tenantId)
                        .externalId(externalId)
                        .title(title)
                        .category(category)
                        .durationSeconds(durationSeconds)
                        .viewCount(views)
                        .likeCount(likes)
                        .shareCount(shares)
                        .avgWatchPercentage(avgWatch)
                        .editorialBoost(editorialBoost)
                        .status("active")
                        .maturityRating("PG")
                        .tags("[\"" + category + "\", \"trending\"]")
                        .publishedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS))
                        .build();
            }

            private List<String> createUserProfiles(Long tenantId) {
                List<String> userIds = new ArrayList<>();

                // User with sports affinity
                String sportsFanId = "a4f2e8c1b9d3e7f6";
                userProfileRepository.save(createUserProfile(tenantId, sportsFanId,
                        50, 180000L, BigDecimal.valueOf(0.75), 20, 5,
                        "{\"sports\": 0.9, \"news\": 0.3, \"comedy\": 0.2}",
                        "[\"sports_001\", \"sports_002\"]"));
                userIds.add(sportsFanId);

                // User with comedy affinity
                String comedyLoverId = "b7c3d9a2e5f1g8h4";
                userProfileRepository.save(createUserProfile(tenantId, comedyLoverId,
                        30, 90000L, BigDecimal.valueOf(0.80), 15, 3,
                        "{\"comedy\": 0.85, \"music\": 0.4, \"sports\": 0.1}",
                        "[\"comedy_001\", \"comedy_003\"]"));
                userIds.add(comedyLoverId);

                // New user (cold start - no watch history)
                String newUserId = "c1d5e9f3a7b2c6d8";
                userProfileRepository.save(createUserProfile(tenantId, newUserId,
                        0, 0L, BigDecimal.ZERO, 0, 0, "{}", "[]"));
                userIds.add(newUserId);

                // User with diverse interests
                String diverseUserId = "d8e2f6a4b1c9d3e7";
                userProfileRepository.save(createUserProfile(tenantId, diverseUserId,
                        100, 300000L, BigDecimal.valueOf(0.70), 40, 10,
                        "{\"sports\": 0.5, \"comedy\": 0.5, \"music\": 0.6, \"news\": 0.3}",
                        "[\"music_001\", \"sports_003\", \"comedy_002\"]"));
                userIds.add(diverseUserId);

                return userIds;
            }

            private UserProfile createUserProfile(Long tenantId, String hashedUserId,
                                                  int watchCount, long watchTimeMs, BigDecimal avgWatch,
                                                  int likes, int shares, String categoryAffinities,
                                                  String lastWatchedIds) {
                return UserProfile.builder()
                        .tenantId(tenantId)
                        .hashedUserId(hashedUserId)
                        .watchCount(watchCount)
                        .totalWatchTimeMs(watchTimeMs)
                        .avgWatchPercentage(avgWatch)
                        .likeCount(likes)
                        .shareCount(shares)
                        .categoryAffinities(categoryAffinities)
                        .lastWatchedIds(lastWatchedIds)
                        .lastActiveAt(Instant.now())
                        .build();
            }
        }
}
