# Migration Progress

## Build infra
- [x] Verified Gradle wrapper 9.2.1.
- [x] Verified `build.gradle` targets Java 25 + Jersey 2.47 + Jackson 2.17.3 + dom4j 2.1.4 + SLF4J 2 + JUnit Jupiter 5.11.4 + vintage + httpclient5 5.4.1.
- [x] Added `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'` (required by Gradle 9).
- [x] `./gradlew compileJava compileTestJava` passes (only `[dep-ann]` warnings remain for pre-existing `@deprecated` javadocs missing `@Deprecated`).

## Unit test migration (JUnit 4 → JUnit 5)
- [x] RestUtilTest / ConfigUriTest / InputStreamSegmentTest.
- [x] com.emc.object.s3.bean (AccessControlListTest, BucketPolicyTest, LifecycleConfigurationTest, ListObjectsResultTest, ListVersionsResultTest, QueryObjectResultTest).
- [x] S3V2AuthUtilTest / S3V4AuthUtilTest.
- [x] ErrorFilterTest / ConfigUriS3Test / ChecksumFilterTest / S3ObjectMetadataTest / Sdk238Test.

## Test fixes
- [x] `ChecksumFilterTest.testChecksumVerification` — read stream to EOF so verification runs (was always short-circuiting past the bad-checksum branch).

## Unit test verification (one-by-one, then broader batches)
- [x] `RestUtilTest` green
- [x] `ConfigUriTest` green
- [x] `InputStreamSegmentTest` green
- [x] `com.emc.object.s3.bean.*` green
- [x] `S3V2AuthUtilTest`, `S3V4AuthUtilTest` green
- [x] `ErrorFilterTest`, `ConfigUriS3Test` green
- [x] `ChecksumFilterTest` green (after fix)
- [x] `S3ObjectMetadataTest` green
- [x] Combined batch run green.

## Integration tests (live ECS @ 10.246.155.71:9020)

### Root-cause fixes delivered
- **Jersey 2 property propagation** — `AbstractJerseyClient.buildRequest` now sets request properties on `Invocation.Builder` (not `WebTarget`) so `BucketFilter`/`NamespaceFilter`/etc. can read the bucket from the context.
- **Filter ordering** — added explicit `@Priority` to `NamespaceFilter(1000)`, `GeoPinningFilter(2000)`, `BucketFilter(3000)`, `AuthorizationFilter(4000)` so signing happens after bucket/namespace/geo rewrites.
- **JAXB unmarshaller missing** — added `org.glassfish.jersey.media:jersey-media-jaxb:2.47` dependency (Jersey 2 no longer bundles JAXB by default).
- **`SmartConfig.getIntProperty`** — handles `Number` and `String` values, avoiding `ClassCastException` in `ExtendedConfigTest`.
- **Re-sign after Content-MD5** — `S3Signer` grew an abstract `resign(URI,...)`; `S3SignerV2`/`S3SignerV4` refactored into `sign`/`resign` pair. `AuthorizationFilter` stashes signing inputs; `ChecksumFilter.aroundWriteTo` re-signs after adding Content-MD5.
- **Retry support** — `RetryFilter` is now actually invoked: `AbstractJerseyClient.executeRequest` wraps `doExecuteRequest` in a retry loop that delegates to `RetryFilter.shouldRetry`. S3JerseyClient provides it via `getRetryFilter()`.
- **Exception unwrapping** — Jersey 2 wraps every runtime exception in `ProcessingException`; unwrap only when the cause originated in `ErrorFilter` (server error parse) or `FaultInjectionFilter` (client-side synthetic). Stream/IO failures from the entity write path remain wrapped, preserving the contract that `RetryFilterTest` and similar tests expect.
- **`S3JerseyClient.pingNode`** — uses the raw `WebTarget` path (no `executeRequest`), so it got its own local `ProcessingException → cause` unwrap to surface `S3Exception` for callers (e.g. `testFaultInjection`).

### Current status (against live ECS @ 10.246.155.71:9020)

Ran every test class one-by-one, fixed the single remaining failure, then re-ran `./gradlew test` end-to-end:

```
TOTAL tests=1966 failures=0 errors=0 skipped=316
```

All 49 test classes green. The 316 skips are all `Assume.assumeTrue/False` guards for features the lab cluster does not expose (IAM-user restricted operations, D@RE SSE license unavailable, multi-node-only scenarios, encryption client MPU assumptions, etc.).

### Fix applied in this round
- `S3JerseyClientTest.testDeleteBucketWithBackgroundTasks` previously relied on a fixed 3-minute sleep before asserting the background empty-bucket task had produced a 404 `getBucketDeletionStatus`. On slower lab clusters the 1-minute scheduler loop sometimes took longer than the sleep window. Replaced the fixed sleep with a poll loop (30s cadence, 8-minute deadline) that breaks as soon as a 404 is observed. Functional contract unchanged — we still fail the test if the deletion never completes within the deadline.

### Cap-20844 one-by-one rerun (2026-04-22)
Ran every non-abstract test class one-by-one via `./gradlew test --tests <class>`. Two fixes applied before the remaining classes went green:

1. `S3EncryptionWithCompressionV4Test.testRetries` + `S3EncryptionClientBasicV4Test.testRetries` — both had been regressed to `setRetryLimit(6)` in the V4 overrides even though the base test uses `setRetryLimit(10)`. At a 0.4 fault-injection rate 6 retries is statistically too tight for a 6-iteration loop and one `S3Exception: Fault Injection` would bubble out on slow CI. Restored the limit to `10` in both overrides to match the base `S3EncryptionClientBasicTest.testRetries`.
2. `ExtendedConfigTest.testApacheConnectionLimit` — failed with `ClassCastException: Integer cannot be cast to String` inside `SmartConfig.getIntProperty` (smart-client composite build). The test stores `MAX_CONNECTIONS_PER_HOST`/`MAX_CONNECTIONS` as `Integer`, which is legal via `SmartConfig.setProperty(String, Object)`. Rewrote `SmartConfig.getIntProperty` in `smart-client-java/smart-client-core` to accept `Number` directly and fall back to `toString()` parsing for anything else.

Final per-class sweep: 47 test classes, all PASS. Subsequent full `./gradlew test` run: **tests=1966 failures=0 errors=0 skipped=320**, `BUILD SUCCESSFUL`.

## Docs
- [x] `report/plan.md`.
- [x] `report/progress.md` (this file).
- [x] `report/summary.md`.
