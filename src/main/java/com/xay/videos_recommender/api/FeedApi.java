package com.xay.videos_recommender.api;

import com.xay.videos_recommender.model.dto.response.ErrorResponse;
import com.xay.videos_recommender.model.dto.response.FeedResponse;
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

@Tag(name = "Feed API", description = "Personalized video feed endpoints")
@RequestMapping("/v1")
public interface FeedApi {

    @Operation(
            summary = "Get personalized feed",
            description = "Returns a personalized video feed for the user based on their watch history, " +
                    "engagement signals, and category affinities. Falls back to trending content for cold-start users."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Feed retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FeedResponse.class))
    )
    @ApiResponse(
            responseCode = "304",
            description = "Not Modified - Client's cached version is still valid",
            content = @Content
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
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
    @GetMapping(value = "/feed", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<FeedResponse> getFeed(
            @Parameter(description = "Tenant identifier", required = true, example = "1")
            @RequestHeader("X-Tenant-ID") Long tenantId,

            @Parameter(description = "Hashed user identifier", required = true, example = "a4f2e8c1b9d3e7f6")
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Request correlation ID for tracing", example = "req-12345")
            @RequestHeader(value = "X-Request-ID", required = false) String requestId,

            @Parameter(description = "ETag from previous response for conditional request", example = "\"1x1\"")
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,

            @Parameter(description = "Maximum number of items to return", example = "5")
            @RequestParam(defaultValue = "5") int limit,

            @Parameter(description = "Pagination cursor from previous response", example = "NQ")
            @RequestParam(required = false) String cursor
    );
}
