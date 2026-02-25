# Migration Plan: Java 25, Gradle 9.2.1, Jersey 2.47

## Phase 1: Build System Migration
1. **Update Gradle Wrapper** - `gradle-wrapper.properties` → Gradle 9.2.1
2. **Update root `build.gradle`**:
   - Replace `maven` plugin with `maven-publish`
   - Remove `net.saliman.cobertura` (incompatible with modern Gradle)
   - Add `jacoco` plugin as replacement
   - Update plugin versions (`dependency-license-report`, `git-publish`, `nebula.release`)
   - Replace `sourceCompatibility = 1.8` with Java 25
   - Replace deprecated `$buildDir` with `layout.buildDirectory`
   - Replace deprecated `pom()` / `uploadJars` / `mavenDeployer` with `maven-publish`
   - Replace `configurations.runtime` with `configurations.runtimeClasspath`
   - Update `signing` to work with `maven-publish`
3. **Update `geo-pin-cli/build.gradle`**:
   - Update `com.github.johnrengelman.shadow` to compatible version
   - Replace deprecated `mainClassName` with `application.mainClass`
   - Replace deprecated `destinationDir` and `classifier`
4. **Update dependencies**:
   - `com.sun.jersey.contribs:jersey-apache-client4:1.19.4` → `org.glassfish.jersey.connectors:jersey-apache-connector:2.47`
   - Add `org.glassfish.jersey.core:jersey-client:2.47`
   - Add `org.glassfish.jersey.inject:jersey-hk2:2.47`
   - Update Jackson to 2.17.x (Jersey 2.x supports JAX-RS 2.x, so no longer stuck on 2.12.x)
   - `junit:junit:4.13.2` → `org.junit.jupiter:junit-jupiter:5.11.4`
   - `org.apache.httpcomponents:httpclient:4.5.13` → `org.apache.httpcomponents.client5:httpclient5:5.4.1`
   - Remove `org.apache.http.params.CoreConnectionPNames` usage (removed in HttpClient 5.x)

## Phase 2: Main Source Code Migration (Jersey 1.x → 2.x)
5. **`ObjectConfig.java`** - Replace `com.sun.jersey.api.client.config.ClientConfig` with `org.glassfish.jersey.client.ClientProperties`, remove `CoreConnectionPNames`
6. **`AbstractJerseyClient.java`** - Replace `Client`, `WebResource`, `ClientResponse`, `ClientConfig`, `ApacheHttpClient4Config` with Jersey 2.x equivalents
7. **`S3Signer.java`, `S3SignerV2.java`, `S3SignerV4.java`** - Replace `ClientRequest` with `ClientRequestContext`
8. **Filter Migration** (8 files):
   - `AuthorizationFilter` → `ClientRequestFilter`
   - `BucketFilter` → `ClientRequestFilter`
   - `NamespaceFilter` → `ClientRequestFilter`
   - `GeoPinningFilter` → `ClientRequestFilter`
   - `FaultInjectionFilter` → `ClientRequestFilter`
   - `ErrorFilter` → `ClientResponseFilter`
   - `RetryFilter` → Move retry logic to `AbstractJerseyClient.executeRequest()`
   - `ChecksumFilter` → `WriterInterceptor` + `ClientResponseFilter`
   - `CodecFilter` → `WriterInterceptor` + `ClientResponseFilter`
9. **`S3JerseyClient.java`** - Update constructor (filter registration via `client.register()`), remove `ClientHandler` parameter, update `destroy()` signature
10. **`S3EncryptionClient.java`** - Update filter insertion logic

## Phase 3: Test Migration
11. **JUnit 4 → JUnit 5** for all 39 test files:
    - `@Test` (org.junit) → `@Test` (org.junit.jupiter.api)
    - `@Before`/`@After` → `@BeforeEach`/`@AfterEach`
    - `@BeforeClass`/`@AfterClass` → `@BeforeAll`/`@AfterAll`
    - `@Ignore` → `@Disabled`
    - `@RunWith` → `@ExtendWith`
    - `Assert.*` → `Assertions.*`
    - `Assume.*` → `Assumptions.*`
    - Update Jersey-specific test imports
12. **Update test Jersey API usage** - Replace `com.sun.jersey` imports

## Phase 4: Verification
13. Attempt Gradle build (`gradlew compileJava compileTestJava`)
14. Fix any remaining compilation errors
15. Generate `progress.md` and `summary.md`

## Key API Mappings
| Jersey 1.x | Jersey 2.x |
|---|---|
| `com.sun.jersey.api.client.Client` | `javax.ws.rs.client.Client` |
| `com.sun.jersey.api.client.WebResource` | `javax.ws.rs.client.WebTarget` |
| `com.sun.jersey.api.client.ClientResponse` | `javax.ws.rs.core.Response` |
| `com.sun.jersey.api.client.filter.ClientFilter` | `javax.ws.rs.client.ClientRequestFilter` / `ClientResponseFilter` |
| `com.sun.jersey.api.client.ClientRequest` | `javax.ws.rs.client.ClientRequestContext` |
| `com.sun.jersey.api.client.ClientHandler` | Removed (use `ConnectorProvider`) |
| `com.sun.jersey.api.client.ClientHandlerException` | `javax.ws.rs.ProcessingException` |
| `com.sun.jersey.api.client.config.ClientConfig` | `org.glassfish.jersey.client.ClientConfig` |
| `ClientConfig.PROPERTY_*` | `org.glassfish.jersey.client.ClientProperties.*` |
| `ApacheHttpClient4Config.PROPERTY_ENABLE_BUFFERING` | `ClientProperties.REQUEST_ENTITY_PROCESSING` with `RequestEntityProcessing.BUFFERED` |
| `client.addFilter(f)` | `client.register(f)` |
| `client.resource(uri)` | `client.target(uri)` |
| `resource.setProperty(k,v)` | `target.property(k,v)` |
| `resource.getRequestBuilder()` | `target.request()` |
| `response.getEntity(Class)` | `response.readEntity(Class)` |
| `response.getEntityInputStream()` | `response.readEntity(InputStream.class)` |
| `response.getHeaders()` | `response.getStringHeaders()` |
| `AbstractClientRequestAdapter` | `WriterInterceptor` |
