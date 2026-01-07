package com.xay.videos_recommender.api;

import com.xay.videos_recommender.model.dto.response.FeedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/v1")
public interface FeedApi {

    @GetMapping("/feed")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<FeedResponse> getFeed(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    );
}

