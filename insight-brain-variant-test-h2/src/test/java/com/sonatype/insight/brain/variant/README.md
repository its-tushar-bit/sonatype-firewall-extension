# Deployment-variant integration tests — three variants, zero hierarchy

Goal: replace the confusing "which base class do I extend?" hierarchy with **one self-documenting
annotation per variant**. You annotate a plain JUnit 5 class and get a running server. No base class.

These tests live in their own modules so `insight-brain-service` and `nexus-mtiq-server` keep their
test trees free of variant-test wiring. There is one module per DB fixture / app variant, all sharing
`insight-brain-variant-test-support` for the launcher/fixture machinery:

- `insight-brain-variant-test-h2` — IQ on embedded H2 (`@IqH2Test`)
- `insight-brain-variant-test-pg` — IQ on embedded PostgreSQL (`@IqPostgresTest`)
- `insight-brain-variant-test-mtiq` — Multi-tenant IQ (`@MtiqTest`)

They are separate modules (not just separate forks) because `DatabaseContainerRule` is a JVM-wide
singleton holding one DB fixture of one type; H2 and Postgres cannot coexist in one JVM. One module
per fixture type means each JVM only ever sees one fixture, which lets each module run
`reuseForks=true` and boot its server **once**, reusing it across all the module's test classes.

## What to use where

| You want to test… | Annotate with | Server | DB |
|---|---|---|---|
| Single-tenant IQ, DB-agnostic (the common case, fastest) | `@IqH2Test` | `SpringTestInsightBrainService` | embedded H2 |
| IQ behaviour that depends on PostgreSQL | `@IqPostgresTest` | `SpringTestInsightBrainService` | embedded PG (zonky) |
| Multi-tenant IQ (tenancy, isolation, MTIQ-only beans) | `@MtiqTest` | `MultiTenantInsightBrainService` | embedded PG (zonky) |

Rule of thumb: **default to `@IqH2Test`.** Reach for `@IqPostgresTest` only for DB-specific behaviour,
and `@MtiqTest` only for tenancy.

## Files

- `IqH2Test`, `IqPostgresTest`, `MtiqTest` — the three variant meta-annotations
- `IqH2ApiSpikeTest`, `IqPostgresApiSpikeTest`, `MtiqApiSpikeTest` — 5 API tests each
- `AbstractSpikeServerExtension` — boots the proven launcher once per variant, caches it JVM-wide,
  injects a `SpikeRestClient`, stops servers via a shutdown hook
- `IqH2ServerExtension`, `IqPostgresServerExtension`, `MtiqServerExtension` — the per-variant
  differences (which DB fixture, service factory, configurator, test-config beans)
- `SpikeSupport` — reusable no-op `Configurator` + post-start base-URL seeding (IQ variants)
- `SpikeRestClient` — JDK `HttpClient` wrapper that does NOT follow redirects (Spring Boot 4 dropped
  `TestRestTemplate`)

## Why this is fast (the whole point)

Each meta-annotation resolves to a single `@ExtendWith(...ServerExtension)`. The extension keys the
running server by variant in a JVM-wide cache, so the three annotations produce **three servers →
booted once each, reused across every test** in the fork. That replaces the old model where the IQ
server restarted (~8s, a full `SpringApplication.run()`) on essentially every test.

The discipline that keeps it at ~3 servers:
1. **One cached server per variant key** — never per test; the shared HTTP server survives between
   `@Test` methods.
2. **No `@DirtiesContext`/`@MockBean`** — nothing changes the cache key.
3. **Reuse the DB fixture, don't recreate the DataSource** — the embedded-postgres cluster and H2
   fixture are singletons, so the server stays cached.

## How to run

```bash
# Install the apps' test-jars + the shared support module first (the variant modules depend on them):
mvn install -pl :insight-brain-service,:nexus-mtiq-server,:insight-brain-variant-test-support -am \
  -DskipTests -Dskip-functional-test -Denforcer.skip=true -Dspotless.check.skip=true

# Then run each variant module (upstream already installed, so no -am — this also avoids the
# unrelated PMD-vs-Java-25 crash that only occurs during a full upstream rebuild):
mvn verify -pl :insight-brain-variant-test-h2   -Dskip-functional-test
mvn verify -pl :insight-brain-variant-test-pg   -Dskip-functional-test
mvn verify -pl :insight-brain-variant-test-mtiq -Dskip-functional-test
```

Each module runs Failsafe with `reuseForks=true forkCount=1`: all of a module's test classes share
one JVM and one booted server, so the ~9s server boot is paid once per module and amortized across
every class. Fixtures never collide because each module uses exactly one `DatabaseContainerRule`
fixture type.

See `docs/build-test-performance-analysis.md` ("Wiring results" and "Isolated module") for the
measured cold-start-once / per-test-reuse numbers and the exact wiring.
