package com.xay.videos_recommender.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xay.videos_recommender.model.domain.ContentCandidate;
import com.xay.videos_recommender.model.domain.RankedVideo;
import com.xay.videos_recommender.model.domain.UserSignals;
import com.xay.videos_recommender.model.entity.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RankingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Default weights if tenant doesn't specify
    private static final double DEFAULT_RECENCY_WEIGHT = 0.3;
    private static final double DEFAULT_ENGAGEMENT_WEIGHT = 0.4;
    private static final double DEFAULT_AFFINITY_WEIGHT = 0.3;

    public List<RankedVideo> rank(List<ContentCandidate> candidates, UserSignals userSignals, Tenant tenant, int limit) {
        Map<String, Double> weights = parseRankingWeights(tenant.getRankingWeights());
        
        return candidates.stream()
                .map(candidate -> scoreCandidate(candidate, userSignals, weights))
                .sorted(Comparator.comparingDouble(RankedVideo::score).reversed())
                .limit(limit)
                .toList();
    }

    public List<RankedVideo> rankWithoutPersonalization(List<ContentCandidate> candidates, int limit) {
        // Just use base score without user signals
        return candidates.stream()
                .map(candidate -> RankedVideo.builder()
                        .videoId(candidate.videoId())
                        .externalId(candidate.externalId())
                        .score(candidate.baseScore().doubleValue() * candidate.editorialBoost().doubleValue())
                        .reason("trending")
                        .build())
                .sorted(Comparator.comparingDouble(RankedVideo::score).reversed())
                .limit(limit)
                .toList();
    }

    private RankedVideo scoreCandidate(ContentCandidate candidate, UserSignals userSignals, Map<String, Double> weights) {
        double recencyWeight = weights.getOrDefault("recency", DEFAULT_RECENCY_WEIGHT);
        double engagementWeight = weights.getOrDefault("engagement", DEFAULT_ENGAGEMENT_WEIGHT);
        double affinityWeight = weights.getOrDefault("affinity", DEFAULT_AFFINITY_WEIGHT);

        double freshnessScore = candidate.freshnessScore().doubleValue();
        double engagementScore = candidate.engagementScore().doubleValue();
        double affinityScore = calculateAffinityScore(candidate, userSignals);
        double editorialBoost = candidate.editorialBoost().doubleValue();

        // Apply penalty for already watched videos
        double watchedPenalty = calculateWatchedPenalty(candidate, userSignals);

        // Calculate final score
        double score = (
                recencyWeight * freshnessScore +
                engagementWeight * engagementScore +
                affinityWeight * affinityScore
        ) * editorialBoost * watchedPenalty;

        String reason = determineReason(affinityScore, freshnessScore, engagementScore, affinityWeight);

        return RankedVideo.builder()
                .videoId(candidate.videoId())
                .externalId(candidate.externalId())
                .score(score)
                .reason(reason)
                .build();
    }

    private double calculateAffinityScore(ContentCandidate candidate, UserSignals userSignals) {
        if (candidate.category() == null || userSignals.categoryAffinities().isEmpty()) {
            return 0.5; // Neutral score if no category or no affinities
        }
        return userSignals.categoryAffinities()
                .getOrDefault(candidate.category(), 0.5);
    }

    private double calculateWatchedPenalty(ContentCandidate candidate, UserSignals userSignals) {
        String videoIdStr = String.valueOf(candidate.videoId());
        if (userSignals.lastWatchedIds().contains(videoIdStr)) {
            return 0.1; // Heavy penalty for already watched
        }
        return 1.0;
    }

    private String determineReason(double affinity, double freshness, double engagement, double affinityWeight) {
        if (affinityWeight > 0.2 && affinity > 0.6) {
            return "category_affinity";
        }
        if (freshness > 0.8) {
            return "new_content";
        }
        if (engagement > 0.7) {
            return "popular";
        }
        return "recommended";
    }

    private Map<String, Double> parseRankingWeights(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return Map.of(
                    "recency", DEFAULT_RECENCY_WEIGHT,
                    "engagement", DEFAULT_ENGAGEMENT_WEIGHT,
                    "affinity", DEFAULT_AFFINITY_WEIGHT
            );
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse ranking weights: {}", json, e);
            return Map.of(
                    "recency", DEFAULT_RECENCY_WEIGHT,
                    "engagement", DEFAULT_ENGAGEMENT_WEIGHT,
                    "affinity", DEFAULT_AFFINITY_WEIGHT
            );
        }
    }
}
