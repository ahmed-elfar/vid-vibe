# Personalized Video Feeds — System Design Document

**Version:** 1.0  
**Date:** January 6, 2025  
**Status:** Ready

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Requirements & Constraints](#2-requirements--constraints)
3. [Architecture Overview](#3-architecture-overview)
4. [Data Model](#4-data-model)
5. [API Contract](#5-api-contract)
6. [Caching Strategy](#6-caching-strategy)
7. [CMS Configuration](#7-cms-configuration)
8. [Event Processing Pipeline](#8-event-processing-pipeline)
9. [Trade-offs & Decisions](#9-trade-offs--decisions)
10. [Rollout & Observability](#10-rollout--observability)
11. [Future Enhancements](#11-future-enhancements)

---

## 1. Executive Summary

This document describes the backend system design for **Personalized Video Feeds**, a feature that serves different video content to users based on their watch history, engagement patterns, and demographic signals.

### Key Design Principles

- **Latency-first**: Precomputation and caching to meet p95 < 250ms
- **Graceful degradation**: Always serve content, fall back to non-personalized if needed
- **Simplicity**: Weighted scoring (no ML in v1), explainable rankings
- **Privacy-conscious**: Only hashed user IDs, no raw PII, 90-day retention

---

## 2. Requirements & Constraints

### Non-Negotiable Constraints

| Constraint | Requirement |
|------------|-------------|
| **Scale** | Peak 3,000 RPS (avg ~600 RPS) |
| **Latency** | p95 < 250ms, p99 < 600ms for 20-item feed |
| **Content Freshness** | New content visible in ≤ 60 seconds |
| **Signal Freshness** | User signal updates can lag ≤ 5 minutes |
| **Privacy** | Hashed user ID only; no raw PII leaves VNet; 90-day event retention |
| **Multi-tenancy** | 120 tenants, each may override global ranking weights |
| **Rollout** | Feature flag with kill switch to non-personalized feed |

---

## 3. Architecture Overview

### 3.1 System Layers

The system is organized into five distinct layers:

**1. Mobile Apps Layer**
- Contains the Mobile SDK embedded in host applications
- Sends hashed user ID (SHA-256) for privacy
- Tracks user events (watches, likes, shares, skips)
- Sends demographic hints once on first connection (for indication only, not used in v1)

**2. API Layer**
- **Feed API** (Java/Spring Boot): Handles `GET /v1/feed` requests, performs personalization ranking, implements cache-first reads with fallback logic
- **Event Ingestion API** (Java/Spring Boot): Handles `POST /v1/events`, performs async processing with schema validation, returns 202 (fire-and-forget)

**3. Data Layer**
- **Redis**: Feed cache, user profile cache, event streams (Redis Streams), tenant config cache
- **PostgreSQL**: Content catalog (videos), aggregated user profiles, tenant configuration (source of truth)
- **S3**: Raw event archive with 90-day retention for compliance and replay capability

**4. Processing Layer**
- **Background Workers**: Event aggregation (every 5 min), affected users feed precomputation, content index rebuild on new videos or CMS updated, S3 archival.

**5. Management Layer**
- **CMS**: Content management, editorial boosts, tenant configuration
- **Feature Flags** (Custom): Kill switch, rollout percentage control

**Data Flow Summary:**
- SDK → Feed API → Redis (cache check) → PostgreSQL (on miss) → Return feed
- SDK → Event Ingestion API → Redis Streams → Background Workers → PostgreSQL + Redis + S3
- CMS → PostgreSQL → Redis (invalidation) → Background Workers (rebuild)

### 3.2 Request Flow — Feed API

**Step-by-step flow for `GET /v1/feed` requests:**

| Step | Action | On Success | On Failure/Skip |
|------|--------|------------|-----------------|
| **1** | Check Feature Flag | Continue to step 2 | Return top N from content_candidates (non-personalized) |
| **2** | Check Feed Cache | If hit + version match → Return cached feed | Continue to step 3 |
| **3** | Get User Profile from cache | Continue to step 4 | Query PostgreSQL, then continue |
| **4** | Cold-start check | If profile exists → Continue to step 5 | Return top N from content_candidates |
| **5** | Get Content Candidates | Continue to step 6 | — |
| **6** | Rank & Score using user profile | Continue to step 7 | — |
| **7** | Cache result & Return | Return personalized feed | If total time > 550ms → Return non-personalized (timeout fallback) |

**Flowchart:**

```json
         SDK Request
              │
              ▼
    ┌─────────────────────┐
    │ 1. Check Feature    │───disabled───► Non-personalized
    │    Flag             │                feed
    └──────────┬──────────┘
               │ enabled
               ▼
    ┌─────────────────────┐
    │ 2. Check Feed       │───hit+valid──► Return cached
    │    Cache            │                feed
    └──────────┬──────────┘
               │ miss
               ▼
    ┌─────────────────────┐
    │ 3. Get User Profile │───miss───► Query PostgreSQL
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │ 4. Cold-start?      │───no profile─► Non-personalized
    └──────────┬──────────┘                feed
               │ has profile
               ▼
    ┌─────────────────────┐
    │ 5. Get Content      │
    │    Candidates       │
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │ 6. Rank & Score     │
    └──────────┬──────────┘
               │
               ▼
    ┌─────────────────────┐
    │ 7. Cache & Return   │───timeout───► Non-personalized
    └──────────┬──────────┘               feed
               │
               ▼
       Personalized Feed
```

---

## 4. Data Model

### 4.1 PostgreSQL Schema

#### Tenants Table

```sql
CREATE TABLE tenants (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    
    -- Ranking Configuration
    ranking_weights         JSONB NOT NULL DEFAULT '{
        "recency": 0.3,
        "engagement": 0.4,
        "affinity": 0.3
    }',
    
    -- Content Filters
    maturity_filter         VARCHAR(16) DEFAULT 'PG-13',
    geo_restrictions        JSONB DEFAULT '[]',
    
    -- Feature Flags
    personalization_enabled BOOLEAN DEFAULT TRUE,
    rollout_percentage      INT DEFAULT 100 CHECK (rollout_percentage BETWEEN 0 AND 100),
    
    -- Metadata
    config_version          INT DEFAULT 1,
    created_at              TIMESTAMP DEFAULT NOW(),
    updated_at              TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_tenants_updated ON tenants(updated_at);
```

#### Videos Table

```sql
CREATE TABLE videos (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenants(id),
    external_id         VARCHAR(128) NOT NULL,
    
    -- Content Metadata
    title               VARCHAR(512) NOT NULL,
    description         TEXT,
    category            VARCHAR(64),
    tags                JSONB DEFAULT '[]',
    duration_seconds    INT,
    maturity_rating     VARCHAR(16) DEFAULT 'G',
    
    -- Engagement Metrics (updated periodically)
    view_count          BIGINT DEFAULT 0,
    like_count          BIGINT DEFAULT 0,
    share_count         BIGINT DEFAULT 0,
    avg_watch_percentage DECIMAL(5,4) DEFAULT 0.0,
    
    -- Editorial Controls
    editorial_boost     DECIMAL(3,2) DEFAULT 1.0,
    is_featured         BOOLEAN DEFAULT FALSE,
    
    -- Status
    status              VARCHAR(16) DEFAULT 'active',
    published_at        TIMESTAMP,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(tenant_id, external_id)
);

CREATE INDEX idx_videos_tenant_status ON videos(tenant_id, status);
CREATE INDEX idx_videos_tenant_published ON videos(tenant_id, published_at DESC);
CREATE INDEX idx_videos_category ON videos(tenant_id, category);
```

#### User Profiles Table

```sql
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
    category_affinities     JSONB DEFAULT '{}',
    -- Example: {"sports": 0.8, "comedy": 0.3, "news": 0.1}
    
    -- Demographic Hints (from SDK, stored once on user creation)
    -- NOTE: For future use only - not used in v1 ranking
    demographic_bucket      VARCHAR(32),
    -- Example: "18-24_US", "25-34_UK"
    
    -- Recency Tracking (JSONB chosen over ARRAY for flexible querying and indexing)
    last_watched_ids        JSONB DEFAULT '[]',
    -- Last 50 video IDs to avoid repeats
    -- Note: JSONB allows GIN indexing and @> containment queries
    -- Alternative: BIGINT[] array, but has limited query flexibility
    
    last_active_at          TIMESTAMP,
    created_at              TIMESTAMP DEFAULT NOW(),
    updated_at              TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(tenant_id, hashed_user_id)
);

-- Critical index for fast lookup
CREATE INDEX idx_user_profiles_lookup 
    ON user_profiles(tenant_id, hashed_user_id);
CREATE INDEX idx_user_profiles_active 
    ON user_profiles(tenant_id, last_active_at DESC);
```

### 4.2 Redis Key Structure

| Key Pattern | Type | Description | Strategy | TTL |
|-------------|------|-------------|----------|-----|
| `tenant:{id}:config` | Hash | Tenant config + feature flags | Read-Through | None |
| `tenant:{id}:content_candidates` | String (JSON) | Pre-sorted content pool with version | Read-Through | None (event-driven eviction) |
| `tenant:{id}:user:{hash}:profile` | Hash | Aggregated user signals | Write-Through / Write-Aside | None (evict on update) |
| `tenant:{id}:user:{hash}:feed` | String (JSON) | Precomputed personalized feed | Cache-Aside | **1 hour** |
| `events:stream:{tenant_id}` | Stream | Raw user events queue | N/A | Until consumed (~5 min) |

#### Content Candidates Structure

```json
{
  "version": 2,
  "updated_at": "2025-01-06T12:00:00Z",
  "candidates": [
    {
      "video_id": "v123",
      "external_id": "abc123",
      "category": "sports",
      "tags": ["football", "highlights"],
      "base_score": 0.95,
      "editorial_boost": 1.5,
      "freshness_score": 0.88,
      "engagement_score": 0.72,
      "maturity_rating": "PG"
    }
  ]
}
```

> Note: `version` starts at 1 and is incremented each time the cache entry is rebuilt.

#### User Feed Structure

```json
{
  "version": 3,
  "generated_at": "2025-01-06T12:05:00Z",
  "feed_type": "personalized",
  "items": [
    {
      "video_id": "v456",
      "score": 0.92,
      "reason": "category_affinity"
    }
  ]
}
```

> Note: `version` starts at 1 and is incremented each time the user's feed is recomputed. The ETag returned to client is `"{candidates_version}x{feed_version}"` (e.g., "2x3").

### 4.3 S3 Storage

| Path Pattern | Content | Retention |
|--------------|---------|-----------|
| `events/{tenant_id}/{date}/{hour}/{batch_id}.json.gz` | Raw user events (compressed) | 90 days |

---

## 5. API Contract

### 5.1 Feed Endpoint

#### Request

`GET /v1/feed?limit=20&cursor={pagination_token}`

**Headers:**

| Header | Required | Description |
|--------|----------|-------------|
| `X-Tenant-ID` | Yes | Tenant identifier |
| `X-User-ID` | Yes | Hashed user identifier (SHA-256) |
| `X-Request-ID` | No | Request tracing ID |
| `If-None-Match` | No | ETag from previous response (empty on first request) |

> **Caching Flow:** On first request, client has no ETag → server returns feed with `ETag` and `Cache-Control: private, max-age=30`. Client caches response for 30 seconds. On subsequent requests within 30s, client uses local cache. After 30s, client sends `If-None-Match` with cached ETag → server returns 304 if unchanged, or 200 with new feed.

#### Response — Success (200 OK)

```json
{
  "items": [
    {
      "video_id": "v123",
      "external_id": "abc123",
      "title": "Amazing Sports Highlights",
      "thumbnail_url": "https://cdn.example.com/thumb/abc123.jpg",
      "duration_seconds": 120,
      "category": "sports"
    }
  ],
  "pagination": {
    "next_cursor": "eyJvZmZzZXQiOjIwfQ==",
    "has_more": true
  },
  "meta": {
    "feed_type": "personalized",
    "generated_at": "2025-01-06T12:00:00Z",
    "ttl_hint_seconds": 30
  }
}
```

> **Note on Pagination:** We use cursor-based pagination instead of limit/offset. The cursor is an opaque, base64-encoded token containing position and feed snapshot state (e.g., last item score and ranking version). This approach provides (1) consistent pagination even as new content arrives, (2) better performance by avoiding large offset scans, and (3) a stable user experience for continuously updated, ranked feeds.

**Response Headers:**

| Header | Value | Description |
|--------|-------|-------------|
| `Cache-Control` | `private, max-age=30` | Client-side cache hint |
| `ETag` | `"2x3"` | Version hash: `{candidates_version}x{feed_version}` |
| `X-Feed-Type` | `personalized` or `fallback` | Indicates feed source |
| `X-Request-ID` | `{request_id}` | Echoed for tracing |

#### Response — Not Modified (304)

Returned when `If-None-Match` header matches current ETag.

#### Response — Fallback Scenarios

| Scenario | HTTP Status | `feed_type` | Behavior |
|----------|-------------|-------------|----------|
| Personalization disabled | 200 | `fallback` | Top N from content_candidates |
| User not in rollout | 200 | `fallback` | Top N from content_candidates |
| Cold-start (no profile) | 200 | `cold_start` | Top N from content_candidates |
| Timeout (> 600ms) | 200 | `timeout_fallback` | Top N from content_candidates |
| Redis unavailable | 200 | `fallback` | Query PostgreSQL directly |

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | `INVALID_REQUEST` | Missing required headers or invalid params |
| 401 | `UNAUTHORIZED` | Invalid or missing authentication |
| 404 | `TENANT_NOT_FOUND` | Unknown tenant ID |
| 429 | `RATE_LIMITED` | Too many requests |
| 500 | `INTERNAL_ERROR` | Unexpected server error |
| 503 | `SERVICE_UNAVAILABLE` | Temporary unavailability |

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Missing required header: X-Tenant-ID",
    "request_id": "req_abc123"
  }
}
```

### 5.2 Events Endpoint

#### Request

`
POST /v1/events
`
`
Content-Type: application/json
`

**Headers:**

| Header | Required | Description |
|--------|----------|-------------|
| `X-Tenant-ID` | Yes | Tenant identifier |
| `X-User-ID` | Yes | Hashed user identifier |
| `X-Request-ID` | No | Request tracing ID |

**Body:**

```json
{
  "events": [
    {
      "type": "video_watch",
      "video_id": "v123",
      "timestamp": "2025-01-06T12:00:00Z",
      "data": {
        "watch_duration_ms": 45000,
        "watch_percentage": 0.75,
        "completed": false
      }
    },
    {
      "type": "video_like",
      "video_id": "v123",
      "timestamp": "2025-01-06T12:00:45Z",
      "data": {}
    }
  ],
  "context": {
    "demographic_bucket": "18-24_US",  // Stored once, for future use only
    "device_type": "mobile",
    "app_version": "2.1.0"
  }
}
```

**Supported Event Types:**

| Type | Description | Data Fields |
|------|-------------|-------------|
| `video_watch` | User watched a video | `watch_duration_ms`, `watch_percentage`, `completed` |
| `video_like` | User liked a video | — |
| `video_unlike` | User removed like | — |
| `video_share` | User shared a video | `share_platform` |
| `video_skip` | User skipped a video | `skip_at_percentage` |
| `feed_scroll` | User scrolled in feed | `position`, `visible_video_ids` |

#### Response — Success (202 Accepted)

```json
{
  "status": "accepted",
  "events_count": 2,
  "request_id": "req_abc123"
}
```

> **Note:** This is a fire-and-forget endpoint. The 202 response indicates events were accepted for processing but not yet persisted.

---

## 6. Caching Strategy

### 6.1 Caching Patterns

#### Read-Through (Tenant Config & Content Candidates)

```json
┌─────────┐     ┌───────┐     ┌────────────┐
│  App    │────►│ Redis │────►│ PostgreSQL │
│         │◄────│       │◄────│            │
└─────────┘     └───────┘     └────────────┘
```
1. App requests data from Redis
2. If cache miss → App queries PostgreSQL
3. App writes result to Redis
4. App returns data


> **Note:** Redis doesn't natively support read-through caching. This is implemented in the application layer with appropriate locking to prevent thundering herd.

#### Write-Through (User Profiles)

```json
┌──────────┐     ┌───────┐
│ Worker   │────►│ Redis │
│          │     └───────┘
│          │     ┌────────────┐
│          │────►│ PostgreSQL │
└──────────┘     └────────────┘

1. Worker aggregates signals
2. Worker writes to PostgreSQL
3. Worker writes to Redis (or evicts for write-aside)
```

#### Cache-Aside (User Feeds)

```json
┌─────────┐     ┌───────┐
│  App    │◄───►│ Redis │
└─────────┘     └───────┘
     │
     ▼ (on miss)
┌─────────┐
│ Compute │
│  Feed   │
└─────────┘

1. App checks Redis for cached feed
2. If miss → Compute feed from profile + candidates
3. App writes computed feed to Redis with TTL
```

### 6.2 Alternative: NCache

**NCache** offers native read-through/write-through support and Pub/Sub messaging via Topics, which could simplify caching logic.

| Feature | Redis | NCache |
|---------|-------|--------|
| Read-Through | Application layer | Native |
| Write-Through | Application layer | Native |
| Pub/Sub | Native | Native (Topics) |
| Streams with Consumer Groups | ✅ Native | ❌ Not available |
| Message Persistence | ✅ Until consumed | ❌ Fire-and-forget |

**Decision:** We chose Redis because:
1. Redis Streams provides durable message queuing with consumer groups
2. We already have Redis in our infrastructure
3. Proven performance at our scale

If using NCache, a separate messaging service (e.g., RabbitMQ) would be needed for event streaming.

### 6.3 Cache Invalidation

#### Content Candidates Invalidation

| Trigger | Action |
|---------|--------|
| CMS adds new video | Evict `tenant:{id}:content_candidates` |
| CMS updates video | Evict `tenant:{id}:content_candidates` |
| CMS deletes video | Evict `tenant:{id}:content_candidates` |

**Flow:**
```json
CMS Update → API returns 200 OK → Background job triggered (async)
                                         │
                                         ▼
                              Rebuild content_candidates
                                         │
                                         ▼
                              Write to Redis with new version
                              (≤ 60s total for freshness SLA)
```

#### User Feed Invalidation (Lazy Rebuild)

When tenant config changes (ranking weights, filters, etc.):

1. Increment `config_version` in PostgreSQL `tenants` table
2. Evict `tenant:{id}:content_candidates` (eager rebuild by background worker)
3. **Do NOT** mass-delete user feeds
4. On next user request: rebuild feed on-demand using new content_candidates
5. Increment `user:feed` version and return new ETag

**Benefits:**
- Tenant content_candidates are rebuilt proactively (eager)
- User feeds are rebuilt lazily on next request (no mass deletion)
- Avoids Redis load spike
- Self-healing on next user request

### 6.4 Versioning Strategy (ETag-Based)

Each cache entry maintains a simple version number, starting at 1 and incremented on each update. These versions are **independent of database fields** — they exist only in the cache layer.

**Version Storage:**

| Cache Entry | Version Field | Incremented When |
|-------------|---------------|------------------|
| `content_candidates` | `version` | CMS adds/updates/deletes content |
| `user:feed` | `version` | User feed is recomputed |

**ETag Composition:**

The API returns an ETag header composed of both versions, separated by "x":

```json
ETag: "{candidates_version}x{feed_version}"

Example: "2x3" means:
  - content_candidates version = 2
  - user feed version = 3
```

**Cache Structures:**

```json
// Content Candidates (tenant-level)
tenant:{id}:content_candidates = {
  "version": 2,        // Starts at 1, incremented on rebuild
  "candidates": [...]
}

// User Feed (user-level)
tenant:{id}:user:{hash}:feed = {
  "version": 3,        // Starts at 1, incremented on rebuild
  "items": [...]
}
```

**Validation Flow:**

1. Client sends `If-None-Match: "2x3"` header
2. Server parses: candidates_version=2, feed_version=3
3. Server checks current versions in cache
4. If both match → return 304 Not Modified
5. If mismatch → return new feed with updated ETag

**Database Sync (Optional):**

For consistency tracking, we store `config_version` in the `tenants` table. This is incremented when tenant config changes (ranking weights, filters) and can be used to trigger cache rebuilds.

```sql
-- On tenant config update:
UPDATE tenants SET config_version = config_version + 1 WHERE id = ?
```

The background worker uses this to detect which tenants need content_candidates rebuilt.

### 6.5 Cache Warming on Startup

On system startup (or after a cold restart), the cache is empty, which would cause a spike in database queries and degraded latency. To mitigate this, we perform **proactive cache warming**:

**Startup Warm-up Sequence:**

| Priority | Action | Criteria |
|----------|--------|----------|
| 1 | Load tenant configs | All active tenants |
| 2 | Build content_candidates | Most active tenants (top 20 by RPS) |
| 3 | Preload user profiles | Most active users per tenant (top 100 by last_active_at) |
| 4 | Precompute personalized feeds | Most active users (top 50 per tenant) |


**Benefits:**
- Reduces cold-start latency spike after deployments
- Ensures most active users get fast responses immediately
- Spreads database load during startup rather than on first requests

**Trade-off:** Startup time increases by ~30-60 seconds, but this is acceptable for smoother user experience.

---

## 7. CMS Configuration

### 7.1 Tenant Configuration UI

Content managers can configure the following per tenant:

#### Ranking Weights

| Weight | Range | Default | Description |
|--------|-------|---------|-------------|
| `recency` | 0.0 - 1.0 | 0.3 | Weight for content freshness |
| `engagement` | 0.0 - 1.0 | 0.4 | Weight for global engagement metrics |
| `affinity` | 0.0 - 1.0 | 0.3 | Weight for user-content affinity |

> Weights must sum to 1.0

#### Content Filters

| Filter | Options | Description |
|--------|---------|-------------|
| `maturity_filter` | G, PG, PG-13, R | Maximum maturity rating to show |
| `geo_restrictions` | Country codes | Restrict content by geography |

#### Editorial Controls

| Control | Description |
|---------|-------------|
| Editorial Boost | Multiply video score by 0.5x - 3.0x |
| Featured Flag | Pin video to top of feed |
| Hide Video | Remove from all feeds |

#### Feature Flags

| Flag | Type | Description |
|------|------|-------------|
| `personalization_enabled` | Boolean | Master kill switch |
| `rollout_percentage` | 0-100 | Gradual rollout percentage |

### 7.2 Configuration Schema

```json
{
  "tenant_id": 123,
  "ranking_weights": {
    "recency": 0.3,
    "engagement": 0.4,
    "affinity": 0.3
  },
  "maturity_filter": "PG-13",
  "geo_restrictions": [],
  "personalization_enabled": true,
  "rollout_percentage": 100,
  "editorial_overrides": {
    "v456": {
      "boost": 2.0,
      "featured": true
    },
    "v789": {
      "hidden": true
    }
  },
  "config_version": 5
}
```

### 7.3 Configuration Update Flow

```json
CMS Config Update
        │
        ▼
┌─────────────────┐
│ Validate Config │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Save to         │
│ PostgreSQL      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Increment       │
│ config_version  │
└────────┬────────┘
         │
         ├──────────────────────────────┐
         ▼                              ▼
┌─────────────────┐          ┌─────────────────┐
│ Evict Redis     │          │ Evict Redis     │
│ tenant:config   │          │ content_        │
│                 │          │ candidates      │
└─────────────────┘          └─────────────────┘
         │
         ▼
┌─────────────────┐
│ User feeds      │
│ invalidated via │
│ lazy versioning │
└─────────────────┘
```

---

## 8. Event Processing Pipeline

### 8.1 Pipeline Overview

```json
┌─────────┐     ┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   SDK   │────►│   Event     │────►│    Redis     │────►│  Background │
│         │     │   Ingestion │     │   Streams    │     │   Workers   │
└─────────┘     │   API       │     │              │     │             │
                └─────────────┘     └──────────────┘     └──────┬──────┘
                                                                │
                    ┌───────────────────────────────────────────┤
                    ▼                   ▼                       ▼
              ┌──────────┐       ┌────────────┐          ┌──────────┐
              │  Redis   │       │ PostgreSQL │          │    S3    │
              │  Cache   │       │            │          │          │
              └──────────┘       └────────────┘          └──────────┘
              (user profiles)    (user profiles)         (raw events)
```

### 8.2 Queue Technology Decision

| Option | Pros | Cons | Verdict |
|--------|------|------|---------|
| **PostgreSQL LISTEN/NOTIFY** | No extra infra | Connection per listener; lock contention under load; no persistence if consumer offline | ❌ Not suitable |
| **Amazon SQS** | Managed, durable | Extra cost; external dependency; adds latency | ❌ Overkill |
| **Kafka** | High throughput, durable | Operational complexity; overkill for 3k RPS | ❌ Overkill |
| **Redis Streams** | Already have Redis; simple; fast; consumer groups | Less durable than Kafka (but we archive to S3) | ✅ Chosen |

**Rationale:** We already have Redis for caching. Redis Streams provides lightweight queuing with consumer group semantics. If Redis fails, we can replay from S3 archives.

### 8.3 Worker Jobs

| Job | Frequency | Description |
|-----|-----------|-------------|
| **Event Aggregation** | Every 5 min | Consume events → aggregate signals → update profiles |
| **Content Index Rebuild** | On Action | Rebuild content_candidates for tenants with changes |
| **S3 Archival** | Every 5 min | Dump consumed events to S3 |

> **Note:** User feeds are **not** precomputed. They are rebuilt lazily on the next user request after profile updates or config changes.
---

## 9. Trade-offs & Decisions

### 9.1 Key Decisions

| Decision | Trade-off | Rationale | Reversible? |
|----------|-----------|-----------|-------------|
| **Redis Streams over Kafka** | Less durable | Simpler ops; S3 backup for replay; sufficient for 3k RPS | ✅ Yes |
| **5-min signal aggregation** | Staleness vs compute | Acceptable per requirements (≤5 min lag allowed) | ✅ Yes |
| **Precomputed feeds + real-time fallback** | Complexity vs latency | Meet p95 <250ms while handling cold users | ✅ Yes |
| **Custom feature flags** | Build vs buy | Simple needs; no external dependency | ✅ Yes |
| **Single PostgreSQL schema** | Less isolation | 120 tenants manageable with tenant_id index | ⚠️ Medium |
| **Weighted scoring (no ML)** | Less sophisticated | Explainable; tunable; fast; collect data for future ML | ✅ Yes |
| **Lazy versioning for user feed invalidation** | Rebuild on next request | Tenant content_candidates rebuilt eagerly (proactive); user feeds rebuilt on-demand (lazy) when requested. Avoids mass deletion and Redis load spikes. | ✅ Yes |
| **1-hour TTL for inactive user feeds** | Stale for returning users | Saves memory; acceptable trade-off | ✅ Yes |

### 9.2 What We Optimized For

1. **Latency**: Precomputation and caching over real-time computation
2. **Reliability**: Fallback-first design; never fail due to personalization
3. **Simplicity**: Explicit weights over black-box ML
4. **Operability**: Clear metrics, kill switch, gradual rollout

### 9.3 What We Deferred

1. **ML-based ranking**: Start with heuristics, collect data first
2. **Real-time signals**: Batch processing sufficient for now
3. **Cross-tenant learning**: Siloed per tenant initially
4. **A/B testing framework**: Use feature flags for now

---

## 10. Rollout & Observability

### 10.1 Rollout Plan

| Phase | Rollout % | Scope | Duration | Success Criteria |
|-------|-----------|-------|----------|------------------|
| **0** | 0% | Deploy dark | 1 day | No errors in logs |
| **1** | 1% | Single tenant, internal | 3 days | p95 < 250ms, no fallback spikes |
| **2** | 10% | One tenant's real traffic | 1 week | CTR improvement signal |
| **3** | 50% | 5 tenants | 2 weeks | Stable metrics |
| **4** | 100% | All tenants | Ongoing | GA with monitoring |

### 10.2 Kill Switch

**Global Kill Switch:**
```json
Redis: system:feature_flags = { "personalization_enabled": false }
```

**Per-Tenant Kill Switch:**
```json
PostgreSQL: UPDATE tenants SET personalization_enabled = false WHERE id = ?
Redis: Evict tenant:{id}:config
```

**Activation Time:** < 60 seconds (next cache refresh)

### 10.3 Metrics & Dashboards

#### Day-1 Metrics

| Metric | Type | Alert Threshold |
|--------|------|-----------------|
| `feed.latency.p95` | Histogram | > 250ms for 5 min |
| `feed.latency.p99` | Histogram | > 600ms for 5 min |
| `feed.type` | Counter (by type) | fallback > 20% |
| `feed.cache.hit_rate` | Gauge | < 80% |
| `events.ingestion.count` | Counter | — |
| `events.ingestion.lag_seconds` | Gauge | > 600s (10 min) |
| `cold_start.rate` | Gauge | — |
| `cold_start.ctr` | Gauge | — |

#### Dashboard Panels

1. **Feed Performance**
   - p50/p95/p99 latency over time
   - RPS by tenant
   - Feed type distribution (personalized vs fallback)

2. **Cache Health**
   - Hit rate by cache type
   - Eviction rate
   - Memory usage

3. **Event Pipeline**
   - Events ingested per minute
   - Processing lag
   - S3 archival status

4. **Business Metrics**
   - CTR by feed type
   - Watch time by feed type
   - Cold-start user engagement

### 10.4 Alerts

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| High Latency | p99 > 600ms for 5 min | P1 | Page on-call |
| High Fallback Rate | fallback > 30% for 10 min | P2 | Investigate |
| Event Lag | lag > 10 min | P2 | Check workers |
| Redis Down | Connection failures | P1 | Page on-call |
| Cache Miss Spike | hit_rate < 50% for 5 min | P2 | Investigate |

---

## 11. Future Enhancements

### 11.1 Knowledge Graph

Model relationships between videos to enable richer recommendations:

```json
Video A ──[same_creator]──► Video B
Video A ──[similar_topic]──► Video C
Video A ──[sequel_of]──► Video D
```

**Benefits:**
- Enable "Because you watched X" explanations
- Better discovery for related content
- Improve cold-start for new videos with known relationships

### 11.2 Content-Based Filtering

Use video metadata (tags, categories, audio/visual features) to recommend similar content:

- **Current (Collaborative):** Users who watched X also watched Y
- **Content-Based:** Video X has similar tags/features to Video Y

**Benefits:**
- Solves cold-start for new videos (no watch history needed)
- Complements collaborative filtering
- More explainable recommendations

### 11.3 Hybrid Recommender

Combine multiple signals for richer recommendations:

```json
Final Score = α(collaborative) + β(content-based) + γ(knowledge-graph) + δ(editorial)
```

### 11.4 Real-Time Analytics via Apache Flink

Stream processing for instant signal updates:

```json
Events → Flink → Real-time Aggregations → Redis
```

**Use Cases:**
- Sub-second trending detection
- Real-time A/B test metrics
- Fraud/bot detection

> **Note:** Likely overkill for current 3k RPS scale, but worth considering as we grow.

### 11.5 Offline Evaluation

Replay historical events to test new ranking algorithms before production:

1. Capture events with displayed feeds
2. Replay with new algorithm
3. Compare predicted vs actual engagement
4. Deploy only if metrics improve

---

## Appendix A: Ranking Algorithm

### Scoring Formula

```java
score = (
    w_recency * freshness_score(video) +
    w_engagement * engagement_score(video) +
    w_affinity * affinity_score(user, video)
) * editorial_boost(video) * diversity_penalty(video, feed)
```

### Component Scores

| Component | Formula | Range |
|-----------|---------|-------|
| `freshness_score` | `exp(-age_hours / 168)` (1-week half-life) | 0.0 - 1.0 |
| `engagement_score` | `normalize(likes + shares * 2 + avg_watch_pct)` | 0.0 - 1.0 |
| `affinity_score` | `user.category_affinities[video.category]` | 0.0 - 1.0 |
| `editorial_boost` | CMS-configured multiplier | 0.5 - 3.0 |
| `diversity_penalty` | `1.0 - 0.1 * same_category_count_in_feed` | 0.5 - 1.0 |

### Penalties

| Penalty | Condition | Multiplier |
|---------|-----------|------------|
| Already watched | `video_id in user.last_watched_ids` | 0.1 |
| Same category saturation | > 3 videos of same category in feed | 0.8 per additional |

---

## Appendix B: API Examples

### Example: Successful Personalized Feed

**Request:**
```http
GET /v1/feed?limit=5 HTTP/1.1
Host: api.example.com
X-Tenant-ID: 123
X-User-ID: sha256_abc123def456
```

**Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json
Cache-Control: private, max-age=30
ETag: "2x3"
X-Feed-Type: personalized

{
  "items": [
    {
      "video_id": "v001",
      "external_id": "sports_highlights_001",
      "title": "Top 10 Goals of the Week",
      "thumbnail_url": "https://cdn.example.com/thumb/001.jpg",
      "duration_seconds": 180,
      "category": "sports"
    },
    {
      "video_id": "v002",
      "external_id": "comedy_sketch_042",
      "title": "Office Prank Gone Wrong",
      "thumbnail_url": "https://cdn.example.com/thumb/002.jpg",
      "duration_seconds": 95,
      "category": "comedy"
    }
  ],
  "pagination": {
    "next_cursor": "eyJvZmZzZXQiOjV9",
    "has_more": true
  },
  "meta": {
    "feed_type": "personalized",
    "generated_at": "2025-01-06T12:00:00Z",
    "ttl_hint_seconds": 30
  }
}
```

### Example: Fallback Feed (Feature Disabled)

**Response:**
```http
HTTP/1.1 200 OK
X-Feed-Type: fallback

{
  "items": [...],
  "meta": {
    "feed_type": "fallback",
    "reason": "personalization_disabled"
  }
}
```

---

## Appendix C: Glossary

| Term | Definition |
|------|------------|
| **Cold-start** | User with no watch history or profile |
| **Content candidates** | Pre-sorted pool of videos eligible for ranking |
| **Affinity score** | How much a user prefers a specific category |
| **Editorial boost** | CMS-configured multiplier to promote content |
| **Lazy versioning** | Invalidation strategy that checks version on read |
| **Write-through** | Cache strategy where writes go to cache and DB simultaneously |
| **Read-through** | Cache strategy where cache fetches from DB on miss |
| **Cache-aside** | Cache strategy where application manages cache explicitly |



