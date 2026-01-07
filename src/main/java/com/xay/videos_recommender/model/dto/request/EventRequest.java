package com.xay.videos_recommender.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

@Schema(description = "Batch of user events for ingestion")
public record EventRequest(
        @NotEmpty(message = "Events list cannot be empty")
        @Size(max = 100, message = "Maximum 100 events per request")
        @Valid
        @Schema(description = "List of user interaction events")
        List<EventItem> events,

        @Size(max = 10, message = "Maximum 10 context fields allowed")
        @Schema(
                description = "Request-level context shared by all events (max 10 fields)",
                example = """
                        {
                          "device_type": "mobile",
                          "app_version": "2.3.1",
                          "session_id": "sess_abc123"
                        }
                        """
        )
        Map<String, String> context
) {}
