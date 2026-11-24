# Test and load report

Date: 24 November 2026. Environment: Windows host, Java 25 runtime, Java 17 compilation target, Spring Boot 3.5.16. Maven used the local installed distribution; tests use H2 in PostgreSQL compatibility mode, Flyway migration and the synthetic `demo` dataset. No Java 17 runtime was available locally. GitHub Actions is configured to run on Java 17, but its result was not available at the time of this report.

## Functional and security results

Command: `mvn clean verify -DexportOpenApi=true`. **BUILD SUCCESS**; 12 discovered tests, 11 executed successfully, zero failures or errors, one skipped (the optional local load test). The 75% line-coverage check passed. The OpenAPI export test generated `openapi.json`; it describes 23 operations and a bearer authentication scheme. The Postman collection includes the same 23 operations.

| Scope | Verified behavior |
|---|---|
| Login and authorization | Valid logins for all three roles; incorrect credentials, missing, malformed, altered, expired and wrong-issuer JWTs rejected; user writes and staff deletes denied; staff create and admin delete allowed. |
| Traceability | Duplicate `B-001` records are independently discoverable; pre/post-reset epochs, late recording, null moisture, material source, machine and quality data are returned. |
| CRUD and input handling | Supplier and batch create/update/delete, line create/delete, filtered and paginated listing, invalid request fields and dates, missing references and duplicate or referenced-row conflicts. |
| Inventory and quality | Lot creation and consumption, insufficient stock rejection, machine-line mismatch rejection, quality timestamp ordering, and machine run creation. |
| Abuse control | Fixed-window counter isolation, expiry and 429 response tested without a proxy. |

`docs/jacoco.xml` captures the functional run. JaCoCo reported **177/190 lines = 93.16%**; **65/94 branches = 69.15%**; **2,203/2,407 instructions = 91.52%**. The project uses compact Java statements, which can inflate line coverage, so the branch figure and behavioral table are also reported. Coverage does not prove absence of defects. The report does not include an actual parallel transaction race test, PostgreSQL integration tests, a security penetration test, fault injection or Docker execution.

## Local HTTP load trial

Separate command: `mvn test -Dtest=LocalLoadTest -DrunLocalLoad=true`. A single Java client and the application ran on the same host against H2, with three seeded batches. After 30 sequential warm-up requests, the client scheduled 1,000 authenticated `GET /api/v1/batches?size=20` requests at one every 5 ms, with a 40-thread client pool. The final run returned:

| Measure | Result |
|---|---:|
| HTTP 200 responses | 1,000 / 1,000 |
| Completed rate | 199.6 requests/second over 5.010 seconds |
| p50 response time | 3.92 ms |
| p95 response time | 7.15 ms |
| p99 response time | 8.37 ms |

Latency is measured from the time a client worker starts its request to receipt of the response; time waiting in the client executor queue is excluded. The test does not cover network distribution, PostgreSQL, Redis, larger catalogs, write contention, sustained load or festival traffic. The measured completion rate is slightly below 200 rps, so the specified 200-rps target is **not established**. The provided k6 constant-arrival-rate script is unrun because k6 is unavailable. Redis caching is not implemented. A representative PostgreSQL dataset and independent load generator are required for the acceptance benchmark.

## Environment limitations

Docker/Compose and PostgreSQL were not available and their configuration has not been executed here. The PostgreSQL SQL dialect, row-lock behavior and container startup require verification on the target environment. Structured logging was configured through Spring Boot; no external log aggregation or audit system was tested. JWT secrets and test passwords in `src/test/resources` are intentionally public and must never be used in deployment.
