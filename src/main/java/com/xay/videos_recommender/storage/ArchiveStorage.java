package com.xay.videos_recommender.storage;

import com.xay.videos_recommender.model.dto.request.EventItem;

import java.util.List;

/**
 * Interface for archiving raw events.
 * In production: S3, GCS, or similar object storage.
 */
public interface ArchiveStorage {

    /**
     * Archives a batch of events for long-term storage.
     */
    void archive(List<EventItem> events);
}
