package com.xay.videos_recommender.service;

import com.xay.videos_recommender.cache.AppCache;
import com.xay.videos_recommender.mapper.UserProfileMapper;
import com.xay.videos_recommender.model.domain.UserSignals;
import com.xay.videos_recommender.model.entity.UserProfile;
import com.xay.videos_recommender.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AppCache appCache;
    private final UserProfileMapper userProfileMapper;

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
                .map(userProfileMapper::toUserSignals)
                .orElse(userProfileMapper.createEmptySignals(tenantId, userId));
    }
}
