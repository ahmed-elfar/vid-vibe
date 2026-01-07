package com.xay.videos_recommender.model.domain;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
public record UserSignals(
        Long tenantId,
        String hashedUserId,
        int watchCount,
        long totalWatchTimeMs,
        BigDecimal avgWatchPercentage,
        int likeCount,
        int shareCount,
        Map<String, Double> categoryAffinities,
        List<String> lastWatchedIds
) {}
