CREATE TABLE tenants (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    
    -- Ranking Configuration
    ranking_weights         TEXT DEFAULT '{"recency": 0.3, "engagement": 0.4, "affinity": 0.3}',
    
    -- Content Filters
    maturity_filter         VARCHAR(16) DEFAULT 'PG-13',
    geo_restrictions        TEXT DEFAULT '[]',
    
    -- Feature Flags
    personalization_enabled BOOLEAN DEFAULT TRUE,
    rollout_percentage      INT DEFAULT 100 CHECK (rollout_percentage BETWEEN 0 AND 100),
    
    -- Metadata
    config_version          INT DEFAULT 1,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenants_updated ON tenants(updated_at);

