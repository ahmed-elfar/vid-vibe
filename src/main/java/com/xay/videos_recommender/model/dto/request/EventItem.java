package com.xay.videos_recommender.model.dto.request;

import java.time.Instant;
import java.util.Map;

public record EventItem(
        String type,
        String videoId,
        Instant timestamp,
        Map<String, Object> data,
        Long tenantId,
        String userId
) {}

