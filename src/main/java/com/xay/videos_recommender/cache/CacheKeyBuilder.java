package com.xay.videos_recommender.cache;

public final class CacheKeyBuilder {

    private CacheKeyBuilder() {
        // Utility class
    }

    public static String tenantConfig(Long tenantId) {
        return "tenant:" + tenantId + ":config";
    }

    public static String contentCandidates(Long tenantId) {
        return "tenant:" + tenantId + ":content_candidates";
    }

    public static String userProfile(Long tenantId, String userId) {
        return "tenant:" + tenantId + ":user:" + userId + ":profile";
    }

    public static String userFeed(Long tenantId, String userId) {
        return "tenant:" + tenantId + ":user:" + userId + ":feed";
    }
}

