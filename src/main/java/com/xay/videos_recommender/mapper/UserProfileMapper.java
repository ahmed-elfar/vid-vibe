package com.xay.videos_recommender.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xay.videos_recommender.model.domain.UserSignals;
import com.xay.videos_recommender.model.entity.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Mapper for converting UserProfile entity to UserSignals domain object.
 */
@Slf4j
@Component
public class UserProfileMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserSignals toUserSignals(UserProfile profile) {
        return UserSignals.builder()
                .tenantId(profile.getTenantId())
                .hashedUserId(profile.getHashedUserId())
                .watchCount(profile.getWatchCount() != null ? profile.getWatchCount() : 0)
                .totalWatchTimeMs(profile.getTotalWatchTimeMs() != null ? profile.getTotalWatchTimeMs() : 0L)
                .avgWatchPercentage(profile.getAvgWatchPercentage() != null ? profile.getAvgWatchPercentage() : BigDecimal.ZERO)
                .likeCount(profile.getLikeCount() != null ? profile.getLikeCount() : 0)
                .shareCount(profile.getShareCount() != null ? profile.getShareCount() : 0)
                .categoryAffinities(parseCategoryAffinities(profile.getCategoryAffinities()))
                .lastWatchedIds(parseLastWatchedIds(profile.getLastWatchedIds()))
                .build();
    }

    public UserSignals createEmptySignals(Long tenantId, String userId) {
        return UserSignals.builder()
                .tenantId(tenantId)
                .hashedUserId(userId)
                .watchCount(0)
                .totalWatchTimeMs(0L)
                .avgWatchPercentage(BigDecimal.ZERO)
                .likeCount(0)
                .shareCount(0)
                .categoryAffinities(Map.of())
                .lastWatchedIds(List.of())
                .build();
    }

    private Map<String, Double> parseCategoryAffinities(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse category affinities: {}", json, e);
            return Map.of();
        }
    }

    private List<String> parseLastWatchedIds(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse last watched IDs: {}", json, e);
            return List.of();
        }
    }
}

