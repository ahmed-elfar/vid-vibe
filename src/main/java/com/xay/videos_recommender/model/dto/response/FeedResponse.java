package com.xay.videos_recommender.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public record FeedResponse(
        List<FeedItem> items,
        PaginationInfo pagination,
        FeedMeta meta,
        @JsonIgnore String eTag
) {}
