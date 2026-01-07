package com.xay.videos_recommender.service;

import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    public boolean isPersonalizationEnabled(Long tenantId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public boolean isUserInRollout(Long tenantId, String userId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public int getRolloutPercentage(Long tenantId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

