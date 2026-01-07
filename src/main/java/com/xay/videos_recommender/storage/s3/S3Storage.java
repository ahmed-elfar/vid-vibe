package com.xay.videos_recommender.storage.s3;

import com.xay.videos_recommender.model.dto.request.EventItem;
import com.xay.videos_recommender.storage.ArchiveStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub S3 implementation for demo purposes.
 * In production: actual S3 SDK integration.
 */
@Slf4j
@Component
public class S3Storage implements ArchiveStorage {

    @Override
    public void archive(List<EventItem> events) {
        if (events.isEmpty()) {
            return;
        }
        // Stub implementation - just log
        log.debug("Archiving {} events to S3 (stub - no actual upload)", events.size());
    }
}
