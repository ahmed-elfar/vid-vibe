package com.xay.videos_recommender.api.controller;

import com.xay.videos_recommender.api.FeedApi;
import com.xay.videos_recommender.model.dto.response.FeedResponse;
import com.xay.videos_recommender.service.FeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedController implements FeedApi {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @Override
    public ResponseEntity<FeedResponse> getFeed(
            Long tenantId,
            String userId,
            String requestId,
            String ifNoneMatch,
            int limit,
            String cursor
    ) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
