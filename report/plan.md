# Java Modernization Plan
**Migration Target:** Java 8 → Java 25, Gradle 6.9.2 → 9.2.1, Jersey 1.19.4 → 2.47

## Analysis Summary

### Current State
- **Java Version:** 1.8
- **Gradle Version:** 6.9.2
- **Jersey Version:** 1.19.4 (com.sun.jersey.contribs:jersey-apache-client4)
- **Test Framework:** JUnit 4.13.2
- **Build System:** Gradle with multiple plugins

### Jersey 1.x Usage Found (36 files)
Main components using Jersey:
- **Client Framework:** AbstractJerseyClient, S3JerseyClient
- **Filters (11):** AuthorizationFilter, BucketFilter, ErrorFilter, FaultInjectionFilter, GeoPinningFilter, NamespaceFilter, RetryFilter, ChecksumFilter, CodecFilter
- **Test Files:** 24 test files using Jersey client APIs

### Key Dependencies to Update
1. Jersey client: `com.sun.jersey` → `org.glassfish.jersey`
2. Jackson: 2.12.7 → 2.18.2 (latest compatible)
3. JUnit: 4.13.2 → 5.11.4
4. Plugin updates for Gradle 9.2.1 compatibility

## Migration Strategy

### Phase 1: Gradle Wrapper Update
- Update gradle-wrapper.properties to 9.2.1
- Test wrapper functionality

### Phase 2: Build Configuration Updates
- Update build.gradle for Java 25
- Update plugin versions for Gradle 9.2.1 compatibility
- Update dependency coordinates for Jersey 2.47
- Update Jackson to compatible version
- Add Jersey 2.x BOM for dependency management

### Phase 3: Jersey 1.x → 2.x API Migration
**Package Changes:**
- `com.sun.jersey.api.client.*` → `jakarta.ws.rs.client.*` and `org.glassfish.jersey.client.*`
- `com.sun.jersey.api.client.config.*` → `org.glassfish.jersey.client.*`
- `com.sun.jersey.client.apache4.*` → `org.glassfish.jersey.apache.connector.*`

**API Changes:**
- `Client` → `javax.ws.rs.client.Client` (use `ClientBuilder.newClient()`)
- `ClientResponse` → `javax.ws.rs.core.Response`
- `WebResource` → `WebTarget`
- `WebResource.Builder` → `Invocation.Builder`
- `ClientFilter` → `ClientRequestFilter` and `ClientResponseFilter`
- `ClientHandler` → Connector API

**Filter Migration:**
- Jersey 1.x filters extend `ClientFilter` with `handle()` method
- Jersey 2.x uses separate `ClientRequestFilter` and `ClientResponseFilter` interfaces

### Phase 4: Test Framework Migration (JUnit 4 → 5)
- Update test dependencies
- Migrate @Test annotations and assertions
- Update test lifecycle annotations (@Before → @BeforeEach, etc.)
- Update assertion imports

### Phase 5: Compilation and Testing
- Fix all compilation errors
- Resolve dependency conflicts
- Run all tests and fix failures
- Verify functional consistency

### Phase 6: Cleanup and Validation
- Remove obsolete dependencies
- Clean up unused imports
- Final build validation
- Generate migration summary

## Risk Mitigation
- Jersey 2.x has significant API changes; each filter requires careful migration
- JAX-RS 2.x uses different request/response model
- Smart client integration needs verification
- Maintain backward compatibility in behavior
