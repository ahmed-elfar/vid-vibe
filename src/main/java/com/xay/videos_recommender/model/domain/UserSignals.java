package com.xay.videos_recommender.model.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UserSignals(
        Long tenantId,
        String userId,
        int watchCount,
        long totalWatchTimeMs,
        BigDecimal avgWatchPercentage,
        int likeCount,
        int shareCount,
        Map<String, Double> categoryAffinities,
        List<String> lastWatchedIds
) {}

