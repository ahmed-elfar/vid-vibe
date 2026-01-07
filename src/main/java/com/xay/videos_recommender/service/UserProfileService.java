package com.xay.videos_recommender.service;

import com.xay.videos_recommender.model.domain.UserSignals;
import com.xay.videos_recommender.model.entity.UserProfile;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

    public Optional<UserProfile> getProfile(Long tenantId, String userId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public UserSignals getUserSignals(Long tenantId, String userId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void updateProfile(Long tenantId, String userId, UserSignals signals) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void incrementWatchCount(Long tenantId, String userId, String videoId, double watchPercentage) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void incrementLikeCount(Long tenantId, String userId, String videoId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void incrementShareCount(Long tenantId, String userId, String videoId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

