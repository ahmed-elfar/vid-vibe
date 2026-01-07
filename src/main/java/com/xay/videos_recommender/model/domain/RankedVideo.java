package com.xay.videos_recommender.model.domain;

public record RankedVideo(
        Long videoId,
        String externalId,
        double score,
        String reason
) {}

