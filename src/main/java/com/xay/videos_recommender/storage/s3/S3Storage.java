package com.xay.videos_recommender.storage.s3;

import com.xay.videos_recommender.model.dto.request.EventItem;
import com.xay.videos_recommender.storage.ArchiveStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class S3Storage implements ArchiveStorage {

    @Override
    public void archive(Long tenantId, List<EventItem> events) {
        // Stub implementation - just log
        log.info("Archiving {} events for tenant {} (stub - no actual S3 upload)", events.size(), tenantId);
    }
}
