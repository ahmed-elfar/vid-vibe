package com.xay.videos_recommender.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_profiles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "hashed_user_id"})
})
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "hashed_user_id", nullable = false)
    private String hashedUserId;

    @Column(name = "watch_count")
    private Integer watchCount = 0;

    @Column(name = "total_watch_time_ms")
    private Long totalWatchTimeMs = 0L;

    @Column(name = "avg_watch_percentage", precision = 5, scale = 4)
    private BigDecimal avgWatchPercentage = BigDecimal.ZERO;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "share_count")
    private Integer shareCount = 0;

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

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getHashedUserId() {
        return hashedUserId;
    }

    public void setHashedUserId(String hashedUserId) {
        this.hashedUserId = hashedUserId;
    }

    public Integer getWatchCount() {
        return watchCount;
    }

    public void setWatchCount(Integer watchCount) {
        this.watchCount = watchCount;
    }

    public Long getTotalWatchTimeMs() {
        return totalWatchTimeMs;
    }

    public void setTotalWatchTimeMs(Long totalWatchTimeMs) {
        this.totalWatchTimeMs = totalWatchTimeMs;
    }

    public BigDecimal getAvgWatchPercentage() {
        return avgWatchPercentage;
    }

    public void setAvgWatchPercentage(BigDecimal avgWatchPercentage) {
        this.avgWatchPercentage = avgWatchPercentage;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getShareCount() {
        return shareCount;
    }

    public void setShareCount(Integer shareCount) {
        this.shareCount = shareCount;
    }

    public String getCategoryAffinities() {
        return categoryAffinities;
    }

    public void setCategoryAffinities(String categoryAffinities) {
        this.categoryAffinities = categoryAffinities;
    }

    public String getDemographicBucket() {
        return demographicBucket;
    }

    public void setDemographicBucket(String demographicBucket) {
        this.demographicBucket = demographicBucket;
    }

    public String getLastWatchedIds() {
        return lastWatchedIds;
    }

    public void setLastWatchedIds(String lastWatchedIds) {
        this.lastWatchedIds = lastWatchedIds;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

