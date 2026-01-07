package com.xay.videos_recommender.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "ranking_weights", columnDefinition = "TEXT")
    private String rankingWeights;

    @Column(name = "maturity_filter")
    private String maturityFilter;

    @Column(name = "geo_restrictions", columnDefinition = "TEXT")
    private String geoRestrictions;

    @Column(name = "personalization_enabled")
    private Boolean personalizationEnabled;

    @Column(name = "rollout_percentage")
    private Integer rolloutPercentage;

    @Column(name = "config_version")
    private Integer configVersion;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
