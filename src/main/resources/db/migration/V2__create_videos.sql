CREATE TABLE videos (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id               BIGINT NOT NULL REFERENCES tenants(id),
    external_id             VARCHAR(128) NOT NULL,
    
    -- Content Metadata
    title                   VARCHAR(512) NOT NULL,
    description             TEXT,
    category                VARCHAR(64),
    tags                    TEXT DEFAULT '[]',
    duration_seconds        INT,
    maturity_rating         VARCHAR(16) DEFAULT 'G',
    
    -- Engagement Metrics (updated periodically)
    view_count              BIGINT DEFAULT 0,
    like_count              BIGINT DEFAULT 0,
    share_count             BIGINT DEFAULT 0,
    avg_watch_percentage    DECIMAL(5,4) DEFAULT 0.0,
    
    -- Editorial Controls
    editorial_boost         DECIMAL(3,2) DEFAULT 1.0,
    is_featured             BOOLEAN DEFAULT FALSE,
    
    -- Status
    status                  VARCHAR(16) DEFAULT 'active',
    published_at            TIMESTAMP,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(tenant_id, external_id)
);

CREATE INDEX idx_videos_tenant_status ON videos(tenant_id, status);
CREATE INDEX idx_videos_tenant_published ON videos(tenant_id, published_at DESC);
CREATE INDEX idx_videos_category ON videos(tenant_id, category);

