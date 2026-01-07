# Personalized Video Feeds - Prototype

A backend system prototype for serving personalized video feeds based on user signals (watch history, engagement, demographic hints).

## Demo Simplifications

This prototype uses simplified dependencies for demo purposes:

| Production Component | Prototype Replacement | Reason |
|---------------------|----------------------|--------|
| **Redis** | Caffeine (in-memory cache) | No external dependencies, simpler setup |
| **PostgreSQL** | H2 (embedded database) | Zero configuration, runs in-memory |
| **S3** | Stub implementation | Focus on core logic, archival is no-op |

The interfaces are designed to easily swap implementations for production.

## Prerequisites

- **Java 21** (required)

## Quick Start

```bash
# Linux/Mac
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

The application will start at `http://localhost:8080`

**On startup, sample data is created and user IDs are printed to the console:**

```
Sample User IDs you can use for testing:
  Tenant 1: [a4f2e8c1b9d3e7f6, b7c3d9a2e5f1g8h4, c1d5e9f3a7b2c6d8, d8e2f6a4b1c9d3e7]
  Tenant 2: [a4f2e8c1b9d3e7f6, b7c3d9a2e5f1g8h4, c1d5e9f3a7b2c6d8, d8e2f6a4b1c9d3e7]
```

| User ID | Description |
|---------|-------------|
| `a4f2e8c1b9d3e7f6` | Sports fan (high sports affinity) |
| `b7c3d9a2e5f1g8h4` | Comedy lover (high comedy affinity) |
| `c1d5e9f3a7b2c6d8` | New user (cold start - no history) |
| `d8e2f6a4b1c9d3e7` | Diverse interests (mixed affinities) |

## Swagger UI

Use the Swagger UI to explore and test all API endpoints interactively:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

The Swagger UI provides:
- Complete API documentation with request/response schemas
- Interactive "Try it out" functionality to test endpoints directly
- Request/response examples

## API Endpoints

### Get Personalized Feed

```bash
curl -X GET "http://localhost:8080/v1/feed?limit=20" \
  -H "X-Tenant-ID: 1" \
  -H "X-User-ID: a4f2e8c1b9d3e7f6"
```

### Post User Events

```bash
curl -X POST "http://localhost:8080/v1/events" \
  -H "X-Tenant-ID: 1" \
  -H "X-User-ID: a4f2e8c1b9d3e7f6" \
  -H "Content-Type: application/json" \
  -d '{
    "events": [
      {
        "type": "video_watch",
        "videoId": "v123",
        "timestamp": "2025-01-06T12:00:00Z",
        "data": {
          "watch_duration_ms": 45000,
          "watch_percentage": 0.75
        }
      }
    ],
    "context": {
      "device_type": "mobile"
    }
  }'
```

## H2 Console

Access the H2 database console at: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:videosdb`
- Username: `sa`
- Password: (empty)

## Health & Metrics (Actuator)

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health status |
| `/actuator/info` | Application info |
| `/actuator/metrics` | List all available metrics |

**Cache metrics available:**
- `/actuator/metrics/cache.gets?tag=cache:feed` — Feed cache hits/misses
- `/actuator/metrics/cache.gets?tag=cache:tenant` — Tenant cache
- `/actuator/metrics/cache.gets?tag=cache:userProfile` — User profile cache
- `/actuator/metrics/cache.gets?tag=cache:contentCandidates` — Content cache

Example:
```bash
# Health check
curl http://localhost:8080/actuator/health

# Feed cache stats
curl "http://localhost:8080/actuator/metrics/cache.gets?tag=cache:feed"
```

## Project Structure

```
src/main/java/com/xay/videos_recommender/
├── api/                        # API interfaces with annotations
│   └── controller/             # Controllers implementing APIs
├── cache/                      # Cache interfaces + implementations
│   ├── AppCache.java           # Interface for app-level caching
│   ├── FeedCacheManager.java   # Interface for feed caching
│   └── caffeine/               # Caffeine implementations
│       ├── CaffeineAppCache.java
│       └── CaffeineFeedCacheManager.java
├── config/                     # Configuration classes
├── exception/                  # Exception handling
├── mapper/                     # Entity to domain object mappers
│   ├── UserProfileMapper.java
│   └── VideoMapper.java
├── model/
│   ├── entity/                 # JPA entities
│   ├── dto/                    # Request/Response records
│   └── domain/                 # Domain value objects (records)
├── repository/                 # Spring Data JPA repositories
├── service/                    # Service classes
│   ├── FeedService.java
│   ├── RankingService.java
│   ├── UserProfileService.java
│   ├── ContentService.java
│   ├── TenantService.java      # Includes feature flag logic
│   └── EventQueueService.java
├── storage/                    # Storage interfaces + implementations
│   ├── ArchiveStorage.java     # Interface
│   └── s3/
│       └── S3Storage.java      # S3 implementation (stub)
├── util/                       # Utility classes
│   ├── CursorUtil.java         # Cursor-based pagination
│   └── ETagUtil.java           # ETag generation/validation
├── worker/                     # Async background workers
└── VideosRecommenderApplication.java
```

## Running Tests

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# All tests
./gradlew test integrationTest
```

## Design Document

See [DESIGN.md](DESIGN.md) for the full system design document.

## License

See [LICENSE](LICENSE) for details.
