package com.xay.videos_recommender.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_user_profiles_lookup", columnList = "tenant_id, hashed_user_id")
})
@Getter
@Setter
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "hashed_user_id", nullable = false)
    private String hashedUserId;

    @Column(name = "watch_count")
    private Integer watchCount;

    @Column(name = "total_watch_time_ms")
    private Long totalWatchTimeMs;

    @Column(name = "avg_watch_percentage", precision = 5, scale = 4)
    private BigDecimal avgWatchPercentage;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "share_count")
    private Integer shareCount;

    @Column(name = "category_affinities", columnDefinition = "TEXT")
    private String categoryAffinities;

    @Column(name = "demographic_bucket")
    private String demographicBucket;

    @Column(name = "last_watched_ids", columnDefinition = "TEXT")
    private String lastWatchedIds;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
