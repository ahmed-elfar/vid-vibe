package com.xay.videos_recommender.worker;

import com.xay.videos_recommender.service.EventQueueService;
import com.xay.videos_recommender.service.UserProfileService;
import com.xay.videos_recommender.storage.ArchiveStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventAggregationWorker {

    private final EventQueueService eventQueueService;
    private final UserProfileService userProfileService;
    private final ArchiveStorage archiveStorage;

    public EventAggregationWorker(
            EventQueueService eventQueueService,
            UserProfileService userProfileService,
            ArchiveStorage archiveStorage
    ) {
        this.eventQueueService = eventQueueService;
        this.userProfileService = userProfileService;
        this.archiveStorage = archiveStorage;
    }

    @Async
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void processEvents() {
        // TODO: Implement
        log.info("EventAggregationWorker: Processing events (not implemented yet)");
    }
}
