package com.xay.videos_recommender.model.domain;

import com.xay.videos_recommender.model.dto.response.FeedItem;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record CachedFeed(
        int version,
        Instant generatedAt,
        String feedType,
        List<FeedItem> items
) {}
