package com.xay.videos_recommender.service;

import com.xay.videos_recommender.model.dto.request.EventItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * In-memory event queue for demo purposes.
 * In production, this would be Redis Streams or similar.
 */
@Slf4j
@Service
public class EventQueueService {

    private final BlockingQueue<EventItem> eventQueue = new LinkedBlockingQueue<>();

    /**
     * Enqueues events with tenant and user context.
     */
    public void enqueue(Long tenantId, String userId, List<EventItem> events) {
        for (EventItem event : events) {
            EventItem enriched = event.withContext(tenantId, userId);
            eventQueue.offer(enriched);
        }
        log.debug("Enqueued {} events for tenant {} user {}", events.size(), tenantId, userId);
    }

    /**
     * Drains up to batchSize events from the queue.
     */
    public List<EventItem> dequeue(int batchSize) {
        List<EventItem> batch = new ArrayList<>(batchSize);
        eventQueue.drainTo(batch, batchSize);
        return batch;
    }

    public int getQueueSize() {
        return eventQueue.size();
    }
}
