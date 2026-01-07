package com.xay.videos_recommender.api;

import com.xay.videos_recommender.model.dto.request.EventRequest;
import com.xay.videos_recommender.model.dto.response.ErrorResponse;
import com.xay.videos_recommender.model.dto.response.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Events API", description = "User event ingestion endpoints")
@RequestMapping("/v1")
public interface EventApi {

    @Operation(
            summary = "Post user events",
            description = "Submit user interaction events (video_watch, video_like, video_share) for processing. " +
                    "Events are queued for async aggregation into user profiles."
    )
    @ApiResponse(
            responseCode = "202",
            description = "Events accepted for processing",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = EventResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid event payload",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Tenant not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
    )
    @PostMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    ResponseEntity<EventResponse> postEvents(
            @Parameter(description = "Tenant identifier", required = true, example = "1")
            @RequestHeader("X-Tenant-ID") Long tenantId,

            @Parameter(description = "Hashed user identifier", required = true, example = "a4f2e8c1b9d3e7f6")
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Request correlation ID for tracing", example = "req-12345")
            @RequestHeader(value = "X-Request-ID", required = false) String requestId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Batch of user events",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EventRequest.class))
            )
            @RequestBody EventRequest eventRequest
    );
}
