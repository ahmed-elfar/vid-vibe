package com.xay.videos_recommender.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xay.videos_recommender.cache.AppCache;
import com.xay.videos_recommender.model.domain.UserSignals;
import com.xay.videos_recommender.model.entity.UserProfile;
import com.xay.videos_recommender.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AppCache appCache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<UserProfile> getProfile(Long tenantId, String userId) {
        return appCache.getUserProfile(tenantId, userId)
                .or(() -> {
                    Optional<UserProfile> profile = userProfileRepository.findByTenantIdAndHashedUserId(tenantId, userId);
                    profile.ifPresent(p -> appCache.putUserProfile(tenantId, userId, p));
                    return profile;
                });
    }

    public UserSignals getUserSignals(Long tenantId, String userId) {
        return getProfile(tenantId, userId)
                .map(this::toUserSignals)
                .orElse(createEmptySignals(tenantId, userId));
    }

    private UserSignals toUserSignals(UserProfile profile) {
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

    private UserSignals createEmptySignals(Long tenantId, String userId) {
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
