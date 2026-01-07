package com.xay.videos_recommender.storage;

import com.xay.videos_recommender.model.dto.request.EventItem;

import java.util.List;

public interface ArchiveStorage {

    void archive(Long tenantId, List<EventItem> events);
}
