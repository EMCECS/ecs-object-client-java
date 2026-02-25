# Next Steps for Migration Completion

## Current Status
- **Phase 1 Complete:** Core infrastructure, filters, and signing mechanisms migrated
- **Dependencies:** Downloaded successfully (Jersey 2.47, Jackson 2.18.2, JUnit 5)
- **Build Status:** Configuration working, compilation has expected errors

## Critical Path to Completion

### 1. Smart-Client Dependency Resolution (BLOCKER)
**Problem:** Project depends on `smart-client-java` which also uses Jersey 1.x

**Options:**
- **A) Migrate smart-client-java** - Update the dependency project for Jersey 2.x
  - Location: `../smart-client-java`
  - Classes needed: `SmartConfig`, `SmartClientFactory`, `LoadBalancer`, `Vdc`, etc.
  - Effort: Medium-High (similar filter migration needed there)

- **B) Use Maven dependency** - If smart-client has a Jersey 2.x compatible version
  - Check Maven Central for compatible version
  - Update dependency coordinates in build.gradle

- **C) Stub out smart-client** - Temporarily disable smart-client features
  - Focus on core S3 client functionality
  - Re-enable after smart-client migration

**Recommendation:** Option A or C depending on project priority

### 2. S3JerseyClient Migration (HIGH PRIORITY)
**File:** `src/main/java/com/emc/object/s3/jersey/S3JerseyClient.java`

**Required Changes:**
```java
// Jersey 1.x
Client client = SmartClientFactory.createStandardClient(smartConfig);
client.addFilter(new ErrorFilter());

// Jersey 2.x
Client client = ClientBuilder.newClient(config);
client.register(new ErrorFilter());
```

**Key Areas:**
- Constructor: Update client creation logic
- Filter registration: Use `client.register()` instead of `addFilter()`
- Smart client integration: Update for Jersey 2.x connector
- Response handling: Already migrated in AbstractJerseyClient

### 3. Complex Filter Migration (MEDIUM PRIORITY)

#### ChecksumFilter
- **Current:** Extends `ClientFilter`, wraps streams in `handle()` method
- **Target:** Implement `WriterInterceptor` + `ReaderInterceptor`
- **Pattern:**
  ```java
  public class ChecksumWriterInterceptor implements WriterInterceptor {
      @Override
      public void aroundWriteTo(WriterInterceptorContext context) {
          // Wrap output stream with checksum calculator
          context.setOutputStream(new ChecksummedOutputStream(...));
          context.proceed();
      }
  }
  ```

#### CodecFilter
- Similar pattern using `WriterInterceptor` for encoding

### 4. Configuration Class Updates (MEDIUM PRIORITY)

#### ObjectConfig.java
- Update `ClientConfig` imports and usage
- Replace Jersey 1.x configuration properties with Jersey 2.x equivalents

#### S3Config.java
- Update `toSmartConfig()` method for Jersey 2.x
- Ensure compatibility with smart-client (after resolution)

### 5. Test Migration (LOW PRIORITY - After Compilation)
- **36 test classes** to migrate from JUnit 4 to JUnit 5
- Pattern:
  ```java
  // JUnit 4
  import org.junit.Test;
  import org.junit.Before;
  @Test public void testMethod() { }
  
  // JUnit 5
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.BeforeEach;
  @Test void testMethod() { }
  ```

## Decision Points

### Should we proceed with smart-client migration?
- **YES** → Start with Option A (migrate smart-client-java)
- **NO** → Use Option C (temporarily disable), focus on core client

### What's the priority?
- **Full Migration** → Complete all components including smart-client
- **Core Functionality** → Get basic S3 client working, defer advanced features

## Estimated Effort

| Component | LOC | Complexity | Effort |
|-----------|-----|------------|--------|
| Smart-client migration | ~2000 | High | 4-6 hours |
| S3JerseyClient | ~800 | Medium | 2-3 hours |
| ChecksumFilter + CodecFilter | ~400 | High | 2-3 hours |
| ObjectConfig + S3Config | ~200 | Medium | 1-2 hours |
| Test migration (36 files) | ~5000 | Low | 3-4 hours |
| Bug fixes + testing | - | Variable | 2-4 hours |
| **TOTAL** | | | **14-22 hours** |

## Recommended Approach

1. **Decision:** Choose smart-client strategy (A, B, or C)
2. **Core Client:** Migrate S3JerseyClient (2-3 hours)
3. **Configuration:** Update ObjectConfig/S3Config (1-2 hours)
4. **Build & Test:** Resolve compilation, run basic tests (1-2 hours)
5. **Advanced Features:** Migrate ChecksumFilter, CodecFilter (2-3 hours)
6. **Full Testing:** JUnit 5 migration and test suite (3-4 hours)

---
**Current Status:** Ready for decision on smart-client strategy
**Blocking:** Smart-client dependency resolution
**Next Action:** Choose Option A, B, or C and proceed accordingly
