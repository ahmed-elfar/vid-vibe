package com.xay.videos_recommender.api.controller;

import com.xay.videos_recommender.api.EventApi;
import com.xay.videos_recommender.model.dto.request.EventRequest;
import com.xay.videos_recommender.model.dto.response.EventResponse;
import com.xay.videos_recommender.service.EventQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EventController implements EventApi {

    private final EventQueueService eventQueueService;

    @Override
    public ResponseEntity<EventResponse> postEvents(
            Long tenantId,
            String userId,
            String requestId,
            @Valid @RequestBody EventRequest eventRequest
    ) {
        eventQueueService.enqueue(tenantId, userId, eventRequest.events());

        String responseRequestId = requestId != null ? requestId : UUID.randomUUID().toString();

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new EventResponse(
                        "accepted",
                        eventRequest.events().size(),
                        responseRequestId
                ));
    }
}
