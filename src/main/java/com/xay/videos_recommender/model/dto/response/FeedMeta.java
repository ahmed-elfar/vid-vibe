package com.xay.videos_recommender.model.dto.response;

import java.time.Instant;

public record FeedMeta(
        String feedType,
        Instant generatedAt,
        int ttlHintSeconds
) {}
