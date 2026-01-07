package com.xay.videos_recommender.api.controller;

import com.xay.videos_recommender.api.EventApi;
import com.xay.videos_recommender.model.dto.request.EventRequest;
import com.xay.videos_recommender.model.dto.response.EventResponse;
import com.xay.videos_recommender.service.EventQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController implements EventApi {

    private final EventQueueService eventQueueService;

    public EventController(EventQueueService eventQueueService) {
        this.eventQueueService = eventQueueService;
    }

    @Override
    public ResponseEntity<EventResponse> postEvents(
            Long tenantId,
            String userId,
            String requestId,
            EventRequest eventRequest
    ) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
