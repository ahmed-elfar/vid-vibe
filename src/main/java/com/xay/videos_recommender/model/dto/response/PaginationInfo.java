package com.xay.videos_recommender.model.dto.response;

public record PaginationInfo(
        String nextCursor,
        boolean hasMore
) {}
