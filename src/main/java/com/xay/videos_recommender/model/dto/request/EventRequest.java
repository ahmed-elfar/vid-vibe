package com.xay.videos_recommender.model.dto.request;

import java.util.List;
import java.util.Map;

public record EventRequest(
        List<EventItem> events,
        Map<String, String> context
) {}

