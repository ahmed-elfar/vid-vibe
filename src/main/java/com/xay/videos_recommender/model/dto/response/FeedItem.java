package com.xay.videos_recommender.model.dto.response;

public record FeedItem(
        String id,
        String externalId,
        String title,
        String thumbnailUrl,
        int durationSeconds,
        String category
) {}
