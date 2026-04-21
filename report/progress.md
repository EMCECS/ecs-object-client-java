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

### Current status (against live ECS)
- `RetryFilterTest` (4/4), `S3IfNoneMatchTest` (1/1), `Sdk238Test` (1/1), `ExtendedConfigTest`, `ChecksumFilterTest`, `ErrorFilterTest`, all unit tests: **green**.
- `S3JerseyClientTest`: **128/135 passing, 12 skipped (IAM-user restricted), 7 failing — all environmental**:
  - `testCreateEncryptedBucket`, `testServerSideEncryption`, `testCopyRangeAPI` → server error `"D@RE jar/license is unavailable"` (missing on the lab cluster).
  - `testCreateObjectWithMetadata`, `testUpdateMetadata`, `testCopyObjectWithMeta` → ECS does not persist `Content-Encoding: none` round-trip (returns `null`).
  - `testMpuAbortInMiddle` → intentional connection abort; Windows socket returns `SocketException: An established connection was aborted by the software in your host machine`, which differs from the Linux-based assertion text.
- `WriteTruncationTest`, `S3EncryptionWithCompressionTest`, `S3EncryptionClientBasicTest`, `S3EncryptionUrlConnectionTest`, `S3EncryptionClientKeyStoreTest` → most tests fail with the same `D@RE jar/license is unavailable` from the server (they all exercise SSE). Not a client migration defect.
- `S3JerseyUrlConnectionTest` → same environmental D@RE/Content-Encoding issues as `S3JerseyClientTest`; also sporadic `OutOfMemoryError: Java heap space` when running large batches back-to-back — raise Gradle JVM args (`-Xmx2g`) if needed.

## Docs
- [x] `report/plan.md`.
- [x] `report/progress.md` (this file).
- [x] `report/summary.md`.
