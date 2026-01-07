package com.xay.videos_recommender.api.controller;

import com.xay.videos_recommender.api.FeedApi;
import com.xay.videos_recommender.model.dto.response.FeedResponse;
import com.xay.videos_recommender.service.ContentService;
import com.xay.videos_recommender.service.FeedService;
import com.xay.videos_recommender.util.ETagUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class FeedController implements FeedApi {

    private final FeedService feedService;
    private final ContentService contentService;

    @Override
    public ResponseEntity<FeedResponse> getFeed(
            Long tenantId,
            String userId,
            String requestId,
            String ifNoneMatch,
            int limit,
            String cursor
    ) {
        FeedResponse response = feedService.generateFeed(tenantId, userId, limit, cursor, ifNoneMatch);
        
        // Handle 304 Not Modified
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        // Build ETag
        int candidatesVersion = contentService.getContentCandidatesVersion(tenantId);
        int feedVersion = candidatesVersion; // Simplified for POC
        String etag = ETagUtil.generate(candidatesVersion, feedVersion);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate())
                .eTag(etag)
                .header("X-Feed-Type", response.meta().feedType())
                .header("X-Request-ID", requestId != null ? requestId : "")
                .body(response);
    }
}
