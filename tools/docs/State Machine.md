# Storage State Machine (STAGING ↔ PERMANENT)

> Module: **Fault Tolerance** · Owner: Resource Service · Status: **Stable** (v1.0)

This document describes the lifecycle states an MP3 resource can occupy as it flows
through the Storage Service–backed upload pipeline. It is the single source of truth
for the state machine; any new transition MUST update this file along with the
schema and the `StorageType` enum.

---

## 1. States

| State       | Description                                                                                               | Persisted in             | Owner            |
|-------------|-----------------------------------------------------------------------------------------------------------|--------------------------|------------------|
| `STAGING`   | File just uploaded by the client; not yet processed by `resource-processor`. Lives in the staging bucket. | `resources.storage_type` | Resource Service |
| `PERMANENT` | File has been processed; metadata extracted; moved to the permanent bucket for long-term storage.         | `resources.storage_type` | Resource Service |

> Only these two values are currently defined. Adding a third value (e.g. `PROCESSING`)
> requires: (a) a new enum constant, (b) a DB migration, (c) update to the
> `StorageResponse.StubConfig` and stub factory, (d) update of this document.

---

## 2. Transitions

```
                +-----------+    processedResource     +-----------+
   upload  -->  |  STAGING  |   (RabbitMQ consumer)    | PERMANENT |
                +-----------+  ----------------------> +-----------+
                       \                                  ^
                        \  (file moved S3 STAGING ->     |
                         \   PERMANENT by                |
                          \  ResourceEventConsumer)      |
                           \                             |
                            +---------------------------+
```

| From        | Trigger                                           | To          | Side effects                                                                                                          |
|-------------|---------------------------------------------------|-------------|-----------------------------------------------------------------------------------------------------------------------|
| (none)      | `POST /resources` upload success                  | `STAGING`   | Insert `resources` row; put object in `staging-bucket/<path>/<key>`; emit `resourceUpload-out-0`.                     |
| `STAGING`   | `processedResource` event from resource-processor | `PERMANENT` | Download from STAGING; upload to PERMANENT; delete from STAGING; update row; idempotent (skips if already PERMANENT). |
| `PERMANENT` | (none, terminal)                                  | `PERMANENT` | Idempotent: repeated `processedResource` events are ignored.                                                          |

---

## 3. Fallback (Storage Service unavailable)

When the Storage Service is unreachable, the circuit breaker opens and the
`StorageServiceClient` returns a `StorageResponse` populated from
`StorageResponse.StubConfig` (see `config-repo/resource-service*.yml`,
`storage.fallback.*` properties). The stubbed response:

* is marked with `stubData=true` (logged & used for observability);
* contains the same DTO shape as the real response (`{id, storageType, bucket, path}`);
* allows the upload flow to continue uninterrupted so the API stays available
  during partial outages.

The `STAGING → PERMANENT` transition is **not** triggered when the consumer
fetches a stub list — the `ResourceEventConsumer` will treat missing
`PERMANENT` storage as a configuration error and surface the underlying cause.

---

## 4. Guarantees

* **At-least-once delivery:** the `processedResource` consumer is idempotent
  (skips when entity is already `PERMANENT`).
* **Bucket ownership:** STAGING and PERMANENT buckets are owned by the
  Storage Service; the Resource Service writes through paths returned by
  `StorageServiceClient` only.
* **No silent data loss:** if a move fails mid-way, the original STAGING
  object is kept and the row is left in `STAGING`; the next `processedResource`
  event will retry the move.

---

## 5. How to extend

To add a new state (e.g. `PROCESSING`):

1. Add the constant to `com.audio.resource.entity.StorageType` and to
   `com.audio.storage.entity.StorageType` (if mirrored).
2. Add a DB migration for the new enum value.
3. Update `StorageResponse.StubConfig` and the stub factory to include a
   default entry for the new state.
4. Update the consumer/producer logic in `ResourceEventConsumer` and
   `ResourceEventPublisher`.
5. Update this document and the `fault_tolerance_descriptor.md`.
6. Add at least one integration test for the new transition.
