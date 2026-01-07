package com.xay.videos_recommender.model.dto.response;

public record FeedItem(
        String videoId,
        String externalId,
        String title,
        String thumbnailUrl,
        Integer durationSeconds,
        String category
) {}

