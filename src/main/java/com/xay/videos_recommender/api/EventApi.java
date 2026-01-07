package com.xay.videos_recommender.api;

import com.xay.videos_recommender.model.dto.request.EventRequest;
import com.xay.videos_recommender.model.dto.response.EventResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/v1")
public interface EventApi {

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ResponseEntity<EventResponse> postEvents(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId,
            @RequestBody EventRequest eventRequest
    );
}

