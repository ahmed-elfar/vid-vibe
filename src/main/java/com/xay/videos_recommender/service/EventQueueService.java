package com.xay.videos_recommender.service;

import com.xay.videos_recommender.model.dto.request.EventItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class EventQueueService {

    private final BlockingQueue<EventItem> eventQueue = new LinkedBlockingQueue<>();

    public void enqueue(Long tenantId, String userId, List<EventItem> events) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<EventItem> dequeue(int batchSize) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public int getQueueSize() {
        return eventQueue.size();
    }
}

