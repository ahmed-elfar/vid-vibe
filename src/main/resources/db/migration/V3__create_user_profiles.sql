CREATE TABLE user_profiles (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id               BIGINT NOT NULL REFERENCES tenants(id),
    hashed_user_id          VARCHAR(64) NOT NULL,
    
    -- Aggregated Engagement Signals
    watch_count             INT DEFAULT 0,
    total_watch_time_ms     BIGINT DEFAULT 0,
    avg_watch_percentage    DECIMAL(5,4) DEFAULT 0.0,
    like_count              INT DEFAULT 0,
    share_count             INT DEFAULT 0,
    
    -- Category Affinities (computed from watch history)
    -- JSONB chosen over ARRAY for flexible querying and GIN indexing
    category_affinities     TEXT DEFAULT '{}',
    
    -- Demographic Hints (from SDK, stored once on user creation)
    -- NOTE: For future use only - not used in v1 ranking
    demographic_bucket      VARCHAR(32),
    
    -- Recency Tracking
    last_watched_ids        TEXT DEFAULT '[]',
    
    last_active_at          TIMESTAMP,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(tenant_id, hashed_user_id)
);

-- Critical index for fast lookup
CREATE INDEX idx_user_profiles_lookup ON user_profiles(tenant_id, hashed_user_id);
CREATE INDEX idx_user_profiles_active ON user_profiles(tenant_id, last_active_at DESC);

