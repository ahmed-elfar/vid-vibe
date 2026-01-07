package com.xay.videos_recommender.worker;

import com.xay.videos_recommender.model.dto.request.EventItem;
import com.xay.videos_recommender.service.EventQueueService;
import com.xay.videos_recommender.storage.ArchiveStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pulls events from queue and processes them.
 * For demo: runs every 500ms to show near real-time processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventAggregationWorker {

    private final EventQueueService eventQueueService;
    private final ArchiveStorage archiveStorage;

    @Value("${app.worker.event-aggregation.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedRateString = "${app.worker.event-aggregation.poll-interval-ms:500}")
    public void processEvents() {
        List<EventItem> events = eventQueueService.dequeue(batchSize);

        if (events.isEmpty()) {
            return;
        }

        log.info("Processing {} events from queue", events.size());

        // Group by user and process
        events.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> e.tenantId() + ":" + e.userId()))
                .forEach((key, userEvents) -> {
                    log.debug("User {}: {} events", key, userEvents.size());
                    // In production: update user profile aggregates
                });

        // Archive to S3 (stub - does nothing in demo)
        archiveStorage.archive(events);

        log.info("Processed and archived {} events", events.size());
    }
}
