# Module 2: Microservices Communication — Implementation Plan

## Context

The task (docs/Communication.md) requires replacing the current synchronous resource-upload flow with an async messaging
pattern. Today, `ResourceService.upload()` calls `SongServiceClient.saveSongMetadata()` directly via WebClient. The new
flow:

1. **resource-service** publishes only `resourceId` to a RabbitMQ queue after upload
2. **resource-processor** consumes the message → GETs binary from resource-service → extracts MP3 metadata → POSTs to
   song-service
3. **Delete path stays synchronous** (resource-service → song-service directly)
4. **Retry** on both the async publish and all sync HTTP calls

resource-processor has **no source files in VCS** (only compiled artifacts in `build/`) — it must be written from
scratch.

---

## Sub-task 1: Add RabbitMQ + resource-service producer

### Files to modify

**`compose.yaml`** — add RabbitMQ service:

```yaml
rabbitmq:
  image: rabbitmq:4-management-alpine
  ports:
    - "5672:5672"
    - "15672:15672"
  environment:
    RABBITMQ_DEFAULT_USER: guest
    RABBITMQ_DEFAULT_PASS: guest
```

**`resource-service/build.gradle`** — add dependencies:

```groovy
implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
implementation 'org.springframework.retry:spring-retry'
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

**`config-service/src/main/resources/configurations/resource-service.yaml`** — add:

```yaml
spring:
  cloud:
    stream:
      bindings:
        resourceUpload-out-0:
          destination: resource-processing
      rabbit:
        bindings:
          resourceUpload-out-0:
            producer:
              autoBindDlq: true
  rabbitmq:
    host: https://rabbitmq-ms-zlj2.onrender.com
    port: 5672
    username: guest
    password: guest
```

**`resource-service/src/main/java/com/audio/resource/service/ResourceService.java`** — in `upload()`, replace:

```java
songServiceClient.saveSongMetadata(metadata);
```

with:

```java
streamBridge.send("resourceUpload-out-0",saved.getId());
```

Inject `StreamBridge` via constructor. Remove `SongMetadataDto` construction from upload path.

**`resource-service/src/main/java/com/audio/resource/service/SongServiceClient.java`** — keep only
`deleteSongMetadata()`, remove `saveSongMetadata()`. Add `@Retryable`:

```java

@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
public void deleteSongMetadata(String ids) { ...}
```

Add `@EnableRetry` to `ResourceApplication`.

---

## Sub-task 2: resource-processor source implementation

resource-processor is a **standalone Gradle project** (not in root `settings.gradle`).

### New source files to create

**`resource-processor/build.gradle`** — add:

```groovy
implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
implementation 'org.apache.tika:tika-core:3.1.0'
implementation 'org.apache.tika:tika-parsers-standard-package:3.1.0'
implementation 'org.springframework.retry:spring-retry'
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

**`ProcessorApplication.java`** — add `@EnableRetry`.

**`dto/SongMetadata.java`** — record or class matching `id`, `name`, `artist`, `album`, `duration`, `year`.

**`service/Mp3MetadataExtractor.java`** — wraps Apache Tika; converts millisecond duration to `mm:ss`; mirrors existing
compiled logic.

**`service/ResourceProcessorService.java`** — core service:

- `@Retryable` `fetchResource(Long id)`: GET `{resource.service.url}/resources/{id}` → `byte[]`
- `@Retryable` `saveSongMetadata(SongMetadata dto)`: POST `{song.service.url}/songs`
- `process(Long resourceId)`: calls both + `Mp3MetadataExtractor`; sets `id = resourceId`

**`config/ResourceProcessorConfig.java`** — `@Bean` `WebClient` instances for resource-service and song-service URLs.

**`messaging/ResourceEventConsumer.java`** — Spring Cloud Stream consumer:

```java

@Bean
public Consumer<Long> processResource() {
    return resourceId -> resourceProcessorService.process(resourceId);
}
```

**`resource-processor/src/main/resources/application.yaml`** — keep existing port 8082, add:

```yaml
spring:
  cloud:
    stream:
      function:
        definition: processResource
      bindings:
        processResource-in-0:
          destination: resource-processing
          group: resource-processor
      rabbit:
        bindings:
          processResource-in-0:
            consumer:
              autoBindDlq: true
              requeueRejected: false
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
resource.service.url: http://localhost:8080
song.service.url: http://localhost:8081
```

**`config-service/src/main/resources/configurations/resource-processor.yaml`** — optional: if resource-processor imports
from config-server, create this file with the above config.

---

## Sub-task 3: Retry mechanism

| Location                                      | Method       | Annotation                                                              |
|-----------------------------------------------|--------------|-------------------------------------------------------------------------|
| `SongServiceClient.deleteSongMetadata()`      | sync HTTP    | `@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000, multiplier=2))` |
| `ResourceProcessorService.fetchResource()`    | sync HTTP    | `@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000, multiplier=2))` |
| `ResourceProcessorService.saveSongMetadata()` | sync HTTP    | `@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000, multiplier=2))` |
| RabbitMQ consumer                             | broker-level | `autoBindDlq: true` + `requeueRejected: false` for DLQ on exhaustion    |

Add `@EnableRetry` on both `ResourceApplication` and `ProcessorApplication`.

---

## File Change Summary

| File                                            | Action                                            |
|-------------------------------------------------|---------------------------------------------------|
| `compose.yaml`                                  | Add RabbitMQ service                              |
| `resource-service/build.gradle`                 | Add stream-rabbit, spring-retry, aop              |
| `config-service/.../resource-service.yaml`      | Add RabbitMQ + stream bindings                    |
| `resource-service/.../ResourceService.java`     | Replace saveSongMetadata with StreamBridge.send   |
| `resource-service/.../SongServiceClient.java`   | Remove saveSongMetadata, add @Retryable to delete |
| `resource-service/.../ResourceApplication.java` | Add @EnableRetry                                  |
| `resource-processor/build.gradle`               | Add stream-rabbit, tika, spring-retry, aop        |
| `resource-processor/src/**` (6 new files)       | Full source implementation                        |
| `config-service/.../resource-processor.yaml`    | New config file for processor service             |

---

## Verification

1. `docker compose up -d` — verify RabbitMQ management UI at `https://rabbitmq-ms-zlj2.onrender.com:15672`
2. Start services in order: config → discovery → resource-service → song-service → resource-processor
3. `POST /resources` with an MP3 → expect `201` with `{"id": N}`
4. Check RabbitMQ UI: message should be consumed from `resource-processing` queue
5. `GET /songs/N` → metadata should be populated by resource-processor
6. `DELETE /resources?id=N` → synchronous cascade; `GET /songs/N` should return 404
7. Kill song-service mid-flight; verify `@Retryable` retries show in resource-processor logs
