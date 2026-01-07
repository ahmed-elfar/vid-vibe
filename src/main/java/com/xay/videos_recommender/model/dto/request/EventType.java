package com.xay.videos_recommender.model.dto.request;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported user event types.
 */
public enum EventType {
    VIDEO_WATCH("video_watch"),
    VIDEO_LIKE("video_like"),
    VIDEO_SHARE("video_share");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

