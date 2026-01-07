package com.xay.videos_recommender.config;

import com.xay.videos_recommender.model.entity.Tenant;
import com.xay.videos_recommender.model.entity.UserProfile;
import com.xay.videos_recommender.model.entity.Video;
import com.xay.videos_recommender.repository.TenantRepository;
import com.xay.videos_recommender.repository.UserProfileRepository;
import com.xay.videos_recommender.repository.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Configuration
public class DataInitializerConfig {

    @Bean
    public CommandLineRunner initData(
            TenantRepository tenantRepository,
            VideoRepository videoRepository,
            UserProfileRepository userProfileRepository
    ) {
        return args -> {
            log.info("Initializing sample data...");

            // Create tenants
            Tenant tenant1 = createTenant("Acme Corp", true, 100);
            Tenant tenant2 = createTenant("Beta Inc", true, 50);
            Tenant tenant3 = createTenant("Gamma LLC", false, 0); // Personalization disabled
            
            tenant1 = tenantRepository.save(tenant1);
            tenant2 = tenantRepository.save(tenant2);
            tenant3 = tenantRepository.save(tenant3);

            log.info("Created tenants: {}, {}, {}", tenant1.getId(), tenant2.getId(), tenant3.getId());

            // Create videos for tenant 1
            createVideosForTenant(videoRepository, tenant1.getId());
            
            // Create videos for tenant 2
            createVideosForTenant(videoRepository, tenant2.getId());

            // Create user profiles
            createUserProfiles(userProfileRepository, tenant1.getId());
            createUserProfiles(userProfileRepository, tenant2.getId());

            log.info("Sample data initialization complete!");
        };
    }

    private Tenant createTenant(String name, boolean personalizationEnabled, int rolloutPercentage) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setPersonalizationEnabled(personalizationEnabled);
        tenant.setRolloutPercentage(rolloutPercentage);
        tenant.setRankingWeights("{\"recency\": 0.3, \"engagement\": 0.4, \"affinity\": 0.3}");
        tenant.setMaturityFilter("PG-13");
        tenant.setConfigVersion(1);
        return tenant;
    }

    private void createVideosForTenant(VideoRepository videoRepository, Long tenantId) {
        // Sports videos
        createVideo(videoRepository, tenantId, "sports_001", "Top 10 Goals of the Week", "sports", 
                180, 10000L, 500L, 50L, BigDecimal.valueOf(0.75), BigDecimal.ONE, 0);
        createVideo(videoRepository, tenantId, "sports_002", "NBA Highlights: Lakers vs Celtics", "sports",
                240, 8000L, 400L, 30L, BigDecimal.valueOf(0.70), BigDecimal.ONE, 1);
        createVideo(videoRepository, tenantId, "sports_003", "F1 Race Recap: Monaco GP", "sports",
                300, 15000L, 800L, 100L, BigDecimal.valueOf(0.80), BigDecimal.valueOf(1.5), 2);

        // Comedy videos
        createVideo(videoRepository, tenantId, "comedy_001", "Office Prank Gone Wrong", "comedy",
                95, 50000L, 2000L, 500L, BigDecimal.valueOf(0.85), BigDecimal.ONE, 3);
        createVideo(videoRepository, tenantId, "comedy_002", "Stand-up: Weekend Special", "comedy",
                600, 30000L, 1500L, 200L, BigDecimal.valueOf(0.65), BigDecimal.ONE, 5);
        createVideo(videoRepository, tenantId, "comedy_003", "Funny Cat Compilation", "comedy",
                120, 100000L, 5000L, 1000L, BigDecimal.valueOf(0.90), BigDecimal.valueOf(1.2), 1);

        // News videos
        createVideo(videoRepository, tenantId, "news_001", "Tech News Roundup", "news",
                180, 5000L, 100L, 20L, BigDecimal.valueOf(0.50), BigDecimal.ONE, 0);
        createVideo(videoRepository, tenantId, "news_002", "Market Update: Stocks Rally", "news",
                120, 3000L, 50L, 10L, BigDecimal.valueOf(0.45), BigDecimal.ONE, 2);

        // Music videos
        createVideo(videoRepository, tenantId, "music_001", "Summer Hits 2025", "music",
                210, 200000L, 10000L, 3000L, BigDecimal.valueOf(0.95), BigDecimal.valueOf(2.0), 0);
        createVideo(videoRepository, tenantId, "music_002", "Acoustic Sessions: Indie Vibes", "music",
                300, 20000L, 1000L, 150L, BigDecimal.valueOf(0.80), BigDecimal.ONE, 4);

        log.info("Created 10 videos for tenant {}", tenantId);
    }

    private void createVideo(VideoRepository videoRepository, Long tenantId, String externalId, 
                            String title, String category, int durationSeconds,
                            long views, long likes, long shares, BigDecimal avgWatch,
                            BigDecimal editorialBoost, int daysAgo) {
        Video video = new Video();
        video.setTenantId(tenantId);
        video.setExternalId(externalId);
        video.setTitle(title);
        video.setCategory(category);
        video.setDurationSeconds(durationSeconds);
        video.setViewCount(views);
        video.setLikeCount(likes);
        video.setShareCount(shares);
        video.setAvgWatchPercentage(avgWatch);
        video.setEditorialBoost(editorialBoost);
        video.setStatus("active");
        video.setMaturityRating("PG");
        video.setTags("[\"" + category + "\", \"trending\"]");
        video.setPublishedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        videoRepository.save(video);
    }

    private void createUserProfiles(UserProfileRepository userProfileRepository, Long tenantId) {
        // User with sports affinity
        createUserProfile(userProfileRepository, tenantId, "a4f2e8c1b9d3e7f6",
                50, 180000L, BigDecimal.valueOf(0.75), 20, 5,
                "{\"sports\": 0.9, \"news\": 0.3, \"comedy\": 0.2}",
                "[\"sports_001\", \"sports_002\"]");

        // User with comedy affinity
        createUserProfile(userProfileRepository, tenantId, "b7c3d9a2e5f1g8h4",
                30, 90000L, BigDecimal.valueOf(0.80), 15, 3,
                "{\"comedy\": 0.85, \"music\": 0.4, \"sports\": 0.1}",
                "[\"comedy_001\", \"comedy_003\"]");

        // New user (cold start - no watch history)
        createUserProfile(userProfileRepository, tenantId, "c1d5e9f3a7b2c6d8",
                0, 0L, BigDecimal.ZERO, 0, 0, "{}", "[]");

        // User with diverse interests
        createUserProfile(userProfileRepository, tenantId, "d8e2f6a4b1c9d3e7",
                100, 300000L, BigDecimal.valueOf(0.70), 40, 10,
                "{\"sports\": 0.5, \"comedy\": 0.5, \"music\": 0.6, \"news\": 0.3}",
                "[\"music_001\", \"sports_003\", \"comedy_002\"]");

        log.info("Created 4 user profiles for tenant {}", tenantId);
    }

    private void createUserProfile(UserProfileRepository userProfileRepository, Long tenantId,
                                   String hashedUserId, int watchCount, long watchTimeMs,
                                   BigDecimal avgWatch, int likes, int shares,
                                   String categoryAffinities, String lastWatchedIds) {
        UserProfile profile = new UserProfile();
        profile.setTenantId(tenantId);
        profile.setHashedUserId(hashedUserId);
        profile.setWatchCount(watchCount);
        profile.setTotalWatchTimeMs(watchTimeMs);
        profile.setAvgWatchPercentage(avgWatch);
        profile.setLikeCount(likes);
        profile.setShareCount(shares);
        profile.setCategoryAffinities(categoryAffinities);
        profile.setLastWatchedIds(lastWatchedIds);
        profile.setLastActiveAt(Instant.now());
        userProfileRepository.save(profile);
    }
}
