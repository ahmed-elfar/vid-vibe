package com.xay.videos_recommender.worker;

import com.xay.videos_recommender.service.ContentService;
import com.xay.videos_recommender.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContentIndexWorker {

    private final ContentService contentService;
    private final TenantService tenantService;

    public ContentIndexWorker(ContentService contentService, TenantService tenantService) {
        this.contentService = contentService;
        this.tenantService = tenantService;
    }

    @Async
    public void rebuildContentIndex(Long tenantId) {
        // TODO: Implement
        log.info("ContentIndexWorker: Rebuilding content index for tenant {} (not implemented yet)", tenantId);
    }
}
