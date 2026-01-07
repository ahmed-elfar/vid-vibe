package com.xay.videos_recommender.model.dto.response;

public record EventResponse(
        String status,
        int eventsCount,
        String requestId
) {}

