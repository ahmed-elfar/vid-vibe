package com.xay.videos_recommender.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "videos", indexes = {
        @Index(name = "idx_videos_tenant_status", columnList = "tenant_id, status")
})
@Getter
@Setter
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "maturity_rating")
    private String maturityRating;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "like_count")
    private Long likeCount;

    @Column(name = "share_count")
    private Long shareCount;

    @Column(name = "avg_watch_percentage", precision = 5, scale = 4)
    private BigDecimal avgWatchPercentage;

    @Column(name = "editorial_boost", precision = 3, scale = 2)
    private BigDecimal editorialBoost;

    @Column(name = "is_featured")
    private Boolean isFeatured;

    @Column(nullable = false)
    private String status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
