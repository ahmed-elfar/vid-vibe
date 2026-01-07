# Personalized Video Feeds - Prototype

A backend system prototype for serving personalized video feeds based on user signals (watch history, engagement, demographic hints).

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

## API Endpoints

### Get Personalized Feed

```bash
curl -X GET "http://localhost:8080/v1/feed?limit=20" \
  -H "X-Tenant-ID: 1" \
  -H "X-User-ID: user123"
```

### Post User Events

```bash
curl -X POST "http://localhost:8080/v1/events" \
  -H "X-Tenant-ID: 1" \
  -H "X-User-ID: user123" \
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

## Project Structure

```
src/main/java/com/xay/videos_recommender/
├── api/                    # API interfaces with annotations
│   └── controller/         # Controllers implementing APIs
├── service/                # Service classes
│   ├── FeedService.java
│   ├── RankingService.java
│   ├── UserProfileService.java
│   ├── ContentService.java
│   ├── TenantService.java
│   ├── FeatureFlagService.java
│   └── EventQueueService.java
├── storage/                # Storage interfaces + implementations
│   ├── ArchiveStorage.java       # Interface
│   └── s3/
│       └── S3Storage.java        # S3 implementation (stub)
├── model/
│   ├── entity/             # JPA entities
│   ├── dto/                # Request/Response records
│   └── domain/             # Domain value objects (records)
├── repository/             # Spring Data JPA repositories
├── worker/                 # Async background workers
├── cache/                  # Cache management
├── config/                 # Configuration classes
├── exception/              # Exception handling
└── util/                   # Utility classes
```

## Design Document

See [DESIGN.md](DESIGN.md) for the full system design document.

## Running Tests

```bash
# Linux/Mac
./gradlew test

# Windows
gradlew.bat test
```

## License

See [LICENSE](LICENSE) for details.
