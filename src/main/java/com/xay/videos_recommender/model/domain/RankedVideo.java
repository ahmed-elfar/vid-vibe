package com.xay.videos_recommender.model.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record RankedVideo(
        Long videoId,
        String externalId,
        double score,
        String reason
) {}
