package com.xay.videos_recommender.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a single user interaction event.
 */
@Schema(description = "A single user interaction event")
public record EventItem(
        @NotNull(message = "Event type is required")
        @Schema(description = "Type of event", example = "video_watch")
        EventType type,

        @NotBlank(message = "Video ID is required")
        @Size(max = 64, message = "Video ID max 64 characters")
        @Schema(description = "The video this event relates to", example = "v123")
        String videoId,

        @NotNull(message = "Timestamp is required")
        @Schema(description = "When the event occurred", example = "2025-01-06T12:00:00Z")
        Instant timestamp,

        @Size(max = 10, message = "Maximum 10 data fields allowed")
        @Schema(
                description = "Event-specific metadata (max 10 fields)",
                example = """
                        {
                          "watch_duration_ms": 45000,
                          "watch_percentage": 0.75
                        }
                        """
        )
        Map<String, Object> data,

        @Schema(hidden = true)
        Long tenantId,

        @Schema(hidden = true)
        String userId
) {
    /**
     * Creates a copy with tenant and user context.
     */
    public EventItem withContext(Long tenantId, String userId) {
        return new EventItem(type, videoId, timestamp, data, tenantId, userId);
    }
}
