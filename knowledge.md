# Project Knowledge: ECS Object Client for Java

## Overview

The **Unified Java Object Client SDK for the ECS Platform** (`ecs-object-client-java`) is
a REST client library that provides Java access to object data on Dell EMC ECS storage
platforms using the **S3 API** (with ECS-specific extensions). It is published to Maven
Central under group `com.emc.ecs` / artifact `object-client`.

- **Group / coordinates:** `com.emc.ecs:object-client`
- **License:** BSD 3-Clause (`LICENSE.txt`)
- **Target JVMs:** Java 8, 11 (documented); build compiles with source/target **Java 17**
- **Javadocs:** http://emcecs.github.io/ecs-object-client-java/latest/javadoc/
- **GitHub:** https://github.com/EMCECS/ecs-object-client-java

> Note (client v3.2+): read timeout is disabled by default (`DEFAULT_READ_TIMEOUT = 0`)
> to avoid conflicts during updates.

## Quick Start

Add the dependency (Gradle):
```groovy
implementation 'com.emc.ecs:object-client:<version>'
```

Create a client and perform basic operations:
```java
import com.emc.object.Protocol;
import com.emc.object.s3.S3Client;
import com.emc.object.s3.S3Config;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.bean.GetObjectResult;

import java.io.InputStream;

// 1. Configure — smart-client against one or more ECS data nodes
S3Config config = new S3Config(Protocol.HTTPS, "10.10.10.11", "10.10.10.12")
        .withIdentity("my_full_token_id")
        .withSecretKey("my_secret_key");
// (optional) set a namespace, switch to V4 signing, tune retries, etc.
// config.withNamespace("my-ns").withUseV2Signer(false);

// 2. Build the client
S3Client s3 = new S3JerseyClient(config);
try {
    // 3. Create a bucket
    if (!s3.bucketExists("my-bucket")) s3.createBucket("my-bucket");

    // 4. Write an object
    s3.putObject("my-bucket", "hello.txt", "Hello World!", "text/plain");

    // 5. Read it back (typed)
    String content = s3.readObject("my-bucket", "hello.txt", String.class);

    // ...or stream it (remember to close the stream)
    GetObjectResult<InputStream> result = s3.getObject("my-bucket", "hello.txt");
    try (InputStream in = result.getObject()) {
        // consume in
    }
} finally {
    // 6. ALWAYS release resources (connection pools, polling threads, host providers)
    s3.destroy();
}
```

Alternative: connect through an external load balancer (disables smart client):
```java
S3Config config = new S3Config(new java.net.URI("https://s3.company.com"))
        .withUseVHost(true)                 // prepend bucket.namespace. to the host
        .withIdentity(id).withSecretKey(key);
```

## Build & Tooling

Built with **Gradle** (wrapper included: `gradlew`, `gradlew.bat`).

- **Root project name:** `object-client` (see `settings.gradle`)
- **Subproject:** `geo-pin-cli` (a standalone CLI tool)
- **Default task:** `distZip` (root), `shadowJar` (geo-pin-cli)

### Key Gradle plugins
- `java-library`, `jacoco` (coverage), `distribution`, `signing`, `maven-publish`
- `com.github.jk1.dependency-license-report` (3rd-party license inventory)
- `org.ajoberstar.git-publish` (publishes Javadocs to `gh-pages`)
- `nebula.release` (release/versioning)

### Common commands
```
./gradlew build            # compile + test
./gradlew test             # run JUnit tests (JUnit Platform, maxHeap 2g)
./gradlew distZip          # build the distribution zip (default)
./gradlew :geo-pin-cli:shadowJar   # build the fat CLI jar
./gradlew javadoc          # generate Javadocs
```

### Publishing
Publishing targets the **Central Portal OSSRH Staging API** (OSSRH is EOL as of Jun 30 2025).
- Requires a Portal **user token** as `sonatypeUser` / `sonatypePass` (not the old OSSRH password).
- `publishToPortal` task POSTs to the manual upload endpoint after `maven-publish` PUTs
  (must originate from the same IP; uses a `Bearer <base64(user:pass)>` header).
- `release` depends on `test`, `publishToPortal`, `gitPublishPush`, `distZip`.
- Signing uses the GPG command line and only runs during the staging publish task.

## Dependencies (notable)

- **`com.emc.ecs:smart-client-ecs:4.0.0`** (api) — client-side load balancing / smart client core
- **Jersey 2.47** (`jersey-client`, `jersey-apache-connector`, `jersey-hk2`, `jersey-media-jaxb`)
  — JAX-RS REST client; always uses the Apache HTTP connector
- **Jackson 2.17.3** — JSON (JAX-RS provider + JAXB annotations)
- **`com.emc.ecs:object-transform:1.1.0`** — encryption/compression codecs
- **dom4j 2.1.4**, **commons-codec 1.17.1**, **slf4j-api 2.0.16**
- **Tests:** JUnit 5 (+ vintage engine), Mockito 5, Apache HttpClient5, log4j2 binding

## Architecture

High-level request flow from your code down to an ECS node:

```
┌──────────────────────────────────────────────────────────────────────┐
│ Your application                                                       │
│    S3Config (+ EncryptionConfig)  ─►  new S3JerseyClient(config)       │
└───────────────────────────────┬──────────────────────────────────────┘
                                 │  S3Client interface (bucket/object ops)
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│ S3JerseyClient  (impl of S3Client, extends AbstractJerseyClient)       │
│  builds request/ objects  ◄──►  JAXB bean/ models (XML)                │
└───────────────────────────────┬──────────────────────────────────────┘
                                 │  JAX-RS (Jersey) request
                                 ▼
        ┌───────────────── Filter pipeline (s3/jersey/) ───────────────┐
        │  GeoPinningFilter  → picks target VDC (GeoPinningUtil)        │
        │  NamespaceFilter / BucketFilter → vhost vs path style         │
        │  ChecksumFilter    → MD5 verify (if checksumEnabled)          │
        │  CodecFilter       → client-side encrypt/compress             │
        │  AuthorizationFilter → sign (S3SignerV2 default / V4)         │
        │  FaultInjectionFilter → optional test faults                 │
        │  ErrorFilter       → map error responses to S3Exception       │
        └───────────────────────────────┬─────────────────────────────┘
                                 │  Apache HTTP connector
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│ smart-client-ecs  → LoadBalancer + EcsHostListProvider                 │
│   (client-side load balancing, health checks, node auto-discovery)     │
└───────────────────────────────┬──────────────────────────────────────┘
                                 ▼
                        ECS data node(s) / VDC(s)
```

Notes:
- With the smart client, the SDK auto-discovers all nodes in a VDC and load-balances across
  healthy ones; the `URI` constructor disables this to route through an external load balancer.
- `S3EncryptionClient` wraps the flow to drive the `CodecFilter` via `EncryptionConfig`.

## Source Layout

```
src/main/java/com/emc/object/
├── AbstractJerseyClient.java   # base REST client behavior
├── ObjectConfig.java           # base config (VDCs, protocol, creds, timeouts, geo-pinning)
├── EncryptionConfig.java       # config for client-side encryption/compression
├── ObjectRequest / ObjectResponse / EntityRequest
├── Method.java, Protocol.java, Range.java
├── s3/                         # S3 API surface
│   ├── S3Client.java           # THE main interface (all S3 operations)
│   ├── S3Config.java           # S3-specific config (extends ObjectConfig)
│   ├── S3Constants, S3Exception, S3ObjectMetadata
│   ├── S3Signer / S3SignerV2 / S3SignerV4   # AWS request signing (v2 default, v4 optional)
│   ├── LargeFileUploader.java  # multipart parallel upload helper
│   ├── LargeFileDownloader.java
│   ├── jersey/                 # implementation + JAX-RS filters
│   ├── request/                # request objects (one per operation/variant)
│   ├── bean/                   # JAXB-annotated request/response model beans
│   └── lfu/                    # large-file-upload support types (resume context, sources)
└── util/                       # checksums, config-uri, progress streams, geo-pinning, REST utils
```

### Core entry points
- **`S3Client`** (`com.emc.object.s3.S3Client`) — the interface every consumer programs against.
  Covers buckets (create/delete/ACL/CORS/lifecycle/policy/versioning/object-lock), objects
  (put/get/read/copy/delete/metadata/tagging/retention/legal-hold), multipart uploads, metadata
  search (`queryObjects`), pre-signed URLs, and ECS extensions (byte-range update, atomic append,
  `copyRange`).
- **`S3JerseyClient`** (`com.emc.object.s3.jersey.S3JerseyClient`) — reference implementation of
  `S3Client` (~940 lines). Extends `AbstractJerseyClient`.
- **`S3EncryptionClient`** (`com.emc.object.s3.jersey.S3EncryptionClient`) — wraps a client to add
  client-side encryption/compression via `EncryptionConfig`.

### Configuration model
- **`ObjectConfig<T>`** (abstract) holds: protocol, list of `Vdc`s, port, smart-client toggle,
  namespace, identity/secretKey, session token, user agent, geo-pinning flags, chunked encoding
  size, connect/read timeouts, and an arbitrary properties map. Timeouts:
  `DEFAULT_CONNECT_TIMEOUT = 15000ms`, `DEFAULT_READ_TIMEOUT = 0` (infinite).
- **`S3Config`** adds S3 specifics: `useVHost`, `signNamespace`, `checksumEnabled`, retry settings
  (`retryEnabled`, `initialRetryDelay`, `retryLimit=3`, `retryBufferSize=2MB`),
  `faultInjectionRate`, `signMetadataSearch`, `useV2Signer` (default **v2**).
  Default ports: HTTP `9020`, HTTPS `9021`.

Three ways to construct an `S3Config`:
1. Smart-client, single VDC: `new S3Config(Protocol.HTTP, "10.10.10.11", "10.10.10.12")`
2. Smart-client, multiple VDCs: `new S3Config(Protocol.HTTPS, vdc1, vdc2)`
3. External load balancer: `new S3Config(URI)` (optionally `.withUseVHost(true)`)

Then supply credentials: `config.withIdentity(id).withSecretKey(key)`.

### JAX-RS filter chain (`s3/jersey/`)
Request/response processing is implemented as a pipeline of filters:
- `AuthorizationFilter` — signs requests (V2/V4)
- `NamespaceFilter` / `BucketFilter` — inject namespace/bucket into host/path (vhost vs path style)
- `ChecksumFilter` — MD5 verification on whole-object reads/writes (when `checksumEnabled`)
- `CodecFilter` — client-side encryption/compression
- `GeoPinningFilter` (+ `GeoPinningRule`) — routes requests to a deterministic VDC based on
  bucket/key hash (see `util/GeoPinningUtil`)
- `ErrorFilter` — converts error responses to `S3Exception`
- `FaultInjectionFilter` — randomly injects HTTP 500s for testing (via `faultInjectionRate`)

### Utilities (`util/`)
- **Checksums:** `RunningChecksum`, `ChecksummedInput/OutputStream`, `ChecksumAlgorithm/Value/Error`
- **`ConfigUri`** — serialize/deserialize a config to/from a URI (properties annotated with
  `@ConfigUriProperty`)
- **Progress:** `ProgressListener`, `ProgressInput/OutputStream`
- **`GeoPinningUtil`** — computes the geo-pin VDC index from a bucket/key GUID
- **Date adapters:** `Iso8601DateAdapter`, `Iso8601DateTimeAdapter`

## geo-pin-cli Subproject

A small standalone CLI (`com.emc.object.s3.GeoPinCli`) that calculates which VDC a request
will be geo-pinned to (given a bucket, optional key, and total VDC count). Uses `commons-cli`
and is packaged as a shaded (fat) jar via the `com.gradleup.shadow` plugin; the artifact is
added to the root distribution's `tools/` directory.

Usage:
```
java -jar geo-pin-cli-<version>.jar -b <bucket-name> [-k <object-key>] -v <vdc-count> [--stacktrace]
```

## Tests

Tests live in `src/test/java/com/emc/object/` and use JUnit 5. Most integration-style tests
run against a real/emulated ECS endpoint (see `AbstractS3ClientTest`, `AbstractClientTest`).
Notable patterns:
- Many suites have a paired **`*V4Test`** subclass that re-runs the same tests using the V4
  signer (e.g. `S3JerseyClientV4Test`, `GeoPinningV4Test`, `LargeFileUploaderV4Test`).
- Broad coverage: `S3JerseyClientTest` (~172 KB) is the primary functional suite; also
  encryption, metadata search, retry, checksum, clock skew, large-file upload/download,
  write truncation, temp credentials, and config-URI tests.

## Build & Test Automation

### Current state
- **Build tool:** Gradle via the wrapper (`gradlew` / `gradlew.bat`); root project `object-client`
  plus the `geo-pin-cli` subproject.
- **Toolchain:** compiles at **Java 17** source/target; documented runtime targets are Java 8/11.
- **No CI pipeline in the repo:** there is no `.github/workflows` (or Jenkinsfile). Build, test,
  and release are run **manually/locally**. Release automation is the single `./gradlew release`
  command (see *Release / Publishing* above and the v4.0.0 Release Runbook in Confluence).
- **Tests are integration tests:** most suites (e.g. `S3JerseyClientTest`) run against a **live
  ECS endpoint or emulator** — they need credentials/config, not just a JVM.

### Build commands
```powershell
# from repo root
.\gradlew.bat assemble               # compile everything (no tests)
.\gradlew.bat distZip                # distribution zip -> build/distributions/  (default task)
.\gradlew.bat :geo-pin-cli:shadowJar # standalone CLI fat jar -> geo-pin-cli/build/shadow/
.\gradlew.bat javadoc                # Javadocs -> build/docs/javadoc/
.\gradlew.bat clean
```

### Test configuration (one-time per environment)
Tests read a `test.properties` file from the test classpath. Create it from the template:
```powershell
Copy-Item src\test\resources\test.properties.template src\test\resources\test.properties
```

Required / optional keys (see `test.properties.template` and
`src/test/java/com/emc/object/util/TestProperties.java`):

| Property | Required | Purpose |
|---|---|---|
| `s3.endpoint` | Yes | `http[s]://<ecshost>[:9020\|:9021]` |
| `s3.access_key` | Yes | S3 access key (`user@domain`) |
| `s3.secret_key` | Yes | S3 secret key |
| `s3.iam_user` | Yes | `true` if the user is an IAM user (enables object-lock paths) |
| `enableVhost` | Optional | `true` for virtual-host-style requests |
| `disableSmartClient` | Optional | `true` when using a load balancer / single node |
| `http.proxyUri` | Optional | route through an HTTP proxy for debugging |
| `s3.temp_access_key` / `s3.temp_secret_key` / `s3.security_token` | Optional | STS/temp-credential tests |

Notes:
- Encryption tests use the checked-in `src/test/resources/keystore.jks` and `keys.properties`.
- Never commit a populated `test.properties` — it is git-ignored.

### Run tests
```powershell
.\gradlew.bat test                                              # full suite (forked JVM, 2g heap)
.\gradlew.bat test --tests "com.emc.object.s3.S3JerseyClientTest"      # single class
.\gradlew.bat test --tests "com.emc.object.s3.S3JerseyClientTest.testPutObject"  # single method
.\gradlew.bat test --tests "*V4Test"                            # V4-signer variants
.\gradlew.bat build                                             # full build + test
```

### Reports & coverage
- **Test results (HTML):** `build/reports/tests/test/index.html`
- **JUnit XML:** `build/test-results/test/`
- **JaCoCo coverage:** `.\gradlew.bat jacocoTestReport` → `build/reports/jacoco/test/html/index.html`

### Recommended automation improvements (gap analysis)
Since there is **no CI today**:
- Add a **GitHub Actions** workflow (`.github/workflows/ci.yml`) running `./gradlew build` on
  push/PR with JDK 17.
- **Separate unit vs integration tests** so PR CI can run fast, endpoint-free checks; gate
  integration tests behind a job with an ECS/emulator + secrets.
- Store test credentials as **CI secrets** and generate `test.properties` at runtime.
- Publish test/JaCoCo reports as workflow artifacts.

## Key Concepts / Gotchas

- **Signer version:** defaults to **V2** (`useV2Signer=true`); set `withUseV2Signer(false)` for V4.
- **Read timeout disabled by default** since v3.2 — set explicitly if you need one.
- **Smart client vs load balancer:** vhost-style requests require disabling the smart client
  (use the `URI` constructor) and setting the namespace appropriately.
- **Content types:** `S3JerseyClient` supports `byte[]`, `String`, `File`/`InputStream`
  (send-only), and JAXB-annotated beans (text/xml, application/xml).
- **ECS extensions** beyond standard S3: byte-range object update, atomic append (returns offset),
  and multi-source `copyRange`.
- **Resource cleanup:** always call `S3Client.destroy()` when done to release connection pools,
  polling threads, and host-list providers.
