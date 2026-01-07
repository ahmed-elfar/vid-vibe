package com.xay.videos_recommender.api.controller;

import com.xay.videos_recommender.api.FeedApi;
import com.xay.videos_recommender.model.dto.response.FeedResponse;
import com.xay.videos_recommender.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class FeedController implements FeedApi {

    private final FeedService feedService;

    @Override
    public ResponseEntity<FeedResponse> getFeed(
            Long tenantId,
            String userId,
            String requestId,
            String ifNoneMatch,
            int limit,
            String cursor
    ) {
        Optional<FeedResponse> response = feedService.generateFeed(tenantId, userId, limit, cursor, ifNoneMatch);
        
        if (response.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        FeedResponse feed = response.get();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate())
                .eTag(feed.eTag())
                .header("X-Feed-Type", feed.meta().feedType())
                .header("X-Request-ID", requestId != null ? requestId : "")
                .body(feed);
    }
}
