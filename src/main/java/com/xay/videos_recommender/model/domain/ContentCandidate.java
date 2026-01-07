package com.xay.videos_recommender.model.domain;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder(toBuilder = true)
public record ContentCandidate(
        Long videoId,
        String externalId,
        String category,
        List<String> tags,
        BigDecimal baseScore,
        BigDecimal editorialBoost,
        BigDecimal freshnessScore,
        BigDecimal engagementScore,
        String maturityRating
) {}
