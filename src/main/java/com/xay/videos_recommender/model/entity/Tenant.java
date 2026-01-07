package com.xay.videos_recommender.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "ranking_weights", columnDefinition = "TEXT")
    private String rankingWeights;

    @Column(name = "maturity_filter")
    private String maturityFilter = "PG-13";

    @Column(name = "geo_restrictions", columnDefinition = "TEXT")
    private String geoRestrictions;

    @Column(name = "personalization_enabled")
    private Boolean personalizationEnabled = true;

    @Column(name = "rollout_percentage")
    private Integer rolloutPercentage = 100;

    @Column(name = "config_version")
    private Integer configVersion = 1;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRankingWeights() {
        return rankingWeights;
    }

    public void setRankingWeights(String rankingWeights) {
        this.rankingWeights = rankingWeights;
    }

    public String getMaturityFilter() {
        return maturityFilter;
    }

    public void setMaturityFilter(String maturityFilter) {
        this.maturityFilter = maturityFilter;
    }

    public String getGeoRestrictions() {
        return geoRestrictions;
    }

    public void setGeoRestrictions(String geoRestrictions) {
        this.geoRestrictions = geoRestrictions;
    }

    public Boolean getPersonalizationEnabled() {
        return personalizationEnabled;
    }

    public void setPersonalizationEnabled(Boolean personalizationEnabled) {
        this.personalizationEnabled = personalizationEnabled;
    }

    public Integer getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(Integer rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    public Integer getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Integer configVersion) {
        this.configVersion = configVersion;
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

