# Migration Plan: ecs-object-client-java

## Target Versions
- **Java**: 8 → 25
- **Gradle**: 6.9.2 → 9.2.1
- **Jersey**: 1.19.4 → 2.47
- **JUnit**: 4.13.2 → 5.x

## Phase 1: Build System Migration
1. Update `gradle/wrapper/gradle-wrapper.properties` to Gradle 9.2.1
2. Update `build.gradle`:
   - Remove deprecated plugins (`maven`, `cobertura`, incompatible `nebula.release`, `org.ajoberstar.git-publish`)
   - Replace `maven` plugin with `maven-publish`
   - Update plugin versions for Gradle 9 compatibility
   - Update `sourceCompatibility` from 1.8 to 25
   - Replace Jersey 1.x dependencies with Jersey 2.47
   - Update Jackson to latest compatible version
   - Replace JUnit 4 with JUnit 5
   - Migrate `uploadJars` / maven deployer to `maven-publish` plugin
   - Replace deprecated `configurations.runtime` with `configurations.runtimeClasspath`
   - Fix `pom()` API removal (use `maven-publish`)
3. Update `geo-pin-cli/build.gradle`:
   - Update shadow plugin to Gradle 9 compatible version
   - Replace deprecated `mainClassName` with `mainClass`
   - Replace deprecated `destinationDir` with `destinationDirectory`
   - Replace deprecated `classifier` with `archiveClassifier`
   - Replace JUnit 4 with JUnit 5
4. Update `settings.gradle` if needed

## Phase 2: Jersey 1.x → 2.x Source Code Migration

### Key API Mapping (Jersey 1 → Jersey 2)
| Jersey 1.x | Jersey 2.x |
|---|---|
| `com.sun.jersey.api.client.Client` | `javax.ws.rs.client.Client` |
| `com.sun.jersey.api.client.WebResource` | `javax.ws.rs.client.WebTarget` |
| `com.sun.jersey.api.client.WebResource.Builder` | `javax.ws.rs.client.Invocation.Builder` |
| `com.sun.jersey.api.client.ClientResponse` | `javax.ws.rs.core.Response` |
| `com.sun.jersey.api.client.ClientRequest` | `org.glassfish.jersey.client.ClientRequest` |
| `com.sun.jersey.api.client.filter.ClientFilter` | `javax.ws.rs.client.ClientRequestFilter` / `javax.ws.rs.client.ClientResponseFilter` |
| `com.sun.jersey.api.client.ClientHandler` | `org.glassfish.jersey.client.spi.ConnectorProvider` |
| `com.sun.jersey.api.client.ClientHandlerException` | `javax.ws.rs.ProcessingException` |
| `com.sun.jersey.api.client.config.ClientConfig` | `org.glassfish.jersey.client.ClientConfig` |
| `com.sun.jersey.client.apache4.config.ApacheHttpClient4Config` | `org.glassfish.jersey.apache.connector.ApacheClientProperties` |
| `ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE` | `ClientProperties.CHUNKED_ENCODING_SIZE` |
| `ClientConfig.PROPERTY_CONNECT_TIMEOUT` | `ClientProperties.CONNECT_TIMEOUT` |
| `ClientConfig.PROPERTY_READ_TIMEOUT` | `ClientProperties.READ_TIMEOUT` |
| `response.getEntity(Type.class)` | `response.readEntity(Type.class)` |
| `response.getEntityInputStream()` | `response.getEntityStream()` (on ClientResponse) |
| `response.getStatusInfo()` | `response.getStatusInfo()` |
| `Client.create()` | `ClientBuilder.newClient()` |
| `client.resource(uri)` | `client.target(uri)` |
| `resource.getRequestBuilder()` | `target.request()` |
| `resource.setProperty()` | `target.property()` / `request.property()` |
| `client.addFilter(filter)` | `client.register(filter)` |
| `client.removeFilter()` | N/A (use ClientConfig registration) |
| `request.getProperties()` | `request.getProperty(name)` / `request.getPropertyNames()` |
| `response.close()` | `response.close()` |

### Files to Migrate (Main Source)
1. `AbstractJerseyClient.java` - Core client base class
2. `S3JerseyClient.java` - Main S3 client implementation
3. `S3EncryptionClient.java` - Encryption client
4. `ObjectConfig.java` - Configuration class
5. `S3Signer.java` - Signer base class
6. `S3SignerV2.java` - V2 signer
7. `S3SignerV4.java` - V4 signer
8. `AuthorizationFilter.java` - Auth filter → ClientRequestFilter
9. `BucketFilter.java` - Bucket filter → ClientRequestFilter
10. `ErrorFilter.java` - Error filter → ClientResponseFilter
11. `NamespaceFilter.java` - Namespace filter → ClientRequestFilter
12. `RetryFilter.java` - Retry filter → ClientRequestFilter
13. `GeoPinningFilter.java` - GeoPinning filter → ClientRequestFilter
14. `FaultInjectionFilter.java` - Fault injection filter → ClientRequestFilter
15. `ChecksumFilter.java` - Checksum filter → ClientRequestFilter + ClientResponseFilter
16. `CodecFilter.java` - Codec filter → ClientRequestFilter + ClientResponseFilter
17. `ConfigUri.java` - Config URI utility

## Phase 3: JUnit 4 → JUnit 5 Test Migration
- Replace `org.junit.Test` → `org.junit.jupiter.api.Test`
- Replace `org.junit.Assert` → `org.junit.jupiter.api.Assertions`
- Replace `org.junit.Assume` → `org.junit.jupiter.api.Assumptions`
- Replace `org.junit.Before` → `org.junit.jupiter.api.BeforeEach`
- Replace `org.junit.After` → `org.junit.jupiter.api.AfterEach`
- Replace `org.junit.BeforeClass` → `org.junit.jupiter.api.BeforeAll`
- Replace `org.junit.AfterClass` → `org.junit.jupiter.api.AfterAll`
- Replace `org.junit.Ignore` → `org.junit.jupiter.api.Disabled`
- Replace `@RunWith` → `@ExtendWith` where applicable
- Update assertion method signatures (parameter order differs)
- Update all Jersey 1.x test imports to Jersey 2.x

## Phase 4: Compilation & Verification
1. Run `./gradlew compileJava` and fix errors iteratively
2. Run `./gradlew compileTestJava` and fix errors iteratively
3. Run `./gradlew test` and verify

## Phase 5: Documentation
1. Update progress.md with completed tasks
2. Generate summary.md
