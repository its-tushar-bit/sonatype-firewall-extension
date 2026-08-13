# insight-brain-variant-test-support

Shared machinery for every `insight-brain-variant-test-*` module. Not a test module
itself — it produces the main-scope classes the sibling modules depend on.

## Intent

House the reused-server / reused-context plumbing in one place so the IQ, MTIQ, and
component variant modules reuse it without duplication. It lives in its own module
because the IQ and MTIQ apps both define Spring `@Configuration` classes in the
`com.sonatype.insight.brain.service` package and collide on a single test classpath —
so each flavour must run in its own module, all pointing back here.

## What lives here (not tests)

- `AbstractSpikeServerExtension` — JVM-wide server-caching JUnit 5 extension base.
- `AbstractIqServerExtension` / `AbstractMtiqServerExtension` — IQ / MTIQ concrete boots.
- `IqTestContext` / `MtiqTestContext` — injected per-test facade (`restRequest()`,
  `lookup()`, `tempEntity()`, license/feature helpers, HDS mock, etc.).
- `ComponentTestDbHarnessExtension` / `ComponentTestPgHarnessExtension` — reproduce the
  ordered `DatabaseContainerRule`/`SearchIndexRule`/`TemporaryEntity`/Mockito lifecycle
  under Jupiter for the component modules.
- `SpikeRestClient` / `SpikeSupport` — redirect-free HTTP client + base-URL seeding.
- `OwnedDatabaseContainerRule` — decouples the variant DB fixture from the legacy singleton.
- `@ComponentH2Test` / `@ComponentPgTest` meta-annotations.

## Build

```bash
mvn install -pl insight-brain-variant-test-support -DskipTests
```
Reinstall this whenever the shared plumbing changes, before running any sibling module.
