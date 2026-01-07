package com.xay.videos_recommender.model.dto.response;

public record ErrorResponse(
        String code,
        String message,
        String requestId
) {}
