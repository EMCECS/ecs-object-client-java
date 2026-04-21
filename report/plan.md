# Migration Plan: ecs-object-client-java

Target: Java 25 (compatible with 17/21), Gradle 9.2.1, Jersey 2.47, using smart-client-ecs 3.1.0-rc.1.

## Current State Assessment (at start)
- `build.gradle` already targets Java 25, uses Jersey 2.47 coordinates, Jackson 2.17.x, dom4j 2.1.4, SLF4J 2.x, JUnit Jupiter 5.11.4 + Vintage engine.
- `settings.gradle` substitutes `smart-client-ecs` via `includeBuild('../smart-client-java')`.
- Gradle wrapper is `9.2.1`.
- `geo-pin-cli/build.gradle` uses Gradle shadow plugin `com.gradleup.shadow:9.0.0-beta4` and JUnit Jupiter.
- Main source compiles successfully (only @Deprecated annotation warnings).
- Test source compiles successfully. `./gradlew test --tests com.emc.object.util.RestUtilTest` runs green.
- All 40+ tests still use JUnit 4 API (`org.junit.Test`, `@Before`, `@After`, `@RunWith(ConcurrentJunitRunner.class)`). They execute under the `junit-vintage-engine`.
- `ConcurrentJunitRunner` is JUnit 4 only.

## Fixes already required and applied
1. Gradle 9 requires an explicit `junit-platform-launcher` test runtime dependency → added to `build.gradle`.

## Planned Steps
1. Verify unit tests (no live ECS needed) pass one-by-one under vintage engine:
   - `com.emc.object.util.RestUtilTest`
   - `com.emc.object.util.ConfigUriTest`
   - `com.emc.object.util.InputStreamSegmentTest`
   - `com.emc.object.s3.bean.*Test` (6 tests)
   - `com.emc.object.s3.S3V2AuthUtilTest`
   - `com.emc.object.s3.S3V4AuthUtilTest`
   - `com.emc.object.s3.S3ObjectMetadataTest`
   - `com.emc.object.s3.ExtendedConfigTest`
   - `com.emc.object.s3.ConfigUriS3Test`
   - `com.emc.object.s3.TestClientRequestContexts`
2. Identify and skip/retain integration tests that require a live ECS endpoint. They will not be executed unless `test.properties` targets a reachable endpoint.
3. Migrate all unit tests from JUnit 4 → JUnit 5 Jupiter (imports, annotations, assertions, lifecycle).
4. Remove `ConcurrentJunitRunner` reliance for unit tests; replace `@RunWith(ConcurrentJunitRunner.class)` on migrated tests with `@Execution(CONCURRENT)` where needed. Keep JUnit 4/vintage engine only while integration tests still rely on it.
5. Re-run the affected test class after each migration batch.
6. Final sanity run: `./gradlew compileJava compileTestJava` and targeted test batches.

## Non-goals
- Migrating integration tests that need live ECS.
- Converting `javax.*` → `jakarta.*` (explicitly disallowed).
- Changing Java/Gradle/Jersey target versions.
