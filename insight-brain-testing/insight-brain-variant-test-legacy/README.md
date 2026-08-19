# insight-brain-variant-test-legacy

Legacy full-server integration tests — the not-yet-converted, slow tests.

## Intent

Holding pen for the slow, boot-heavy tests we **haven't been able to convert** to the
reused-server / reused-context variant pattern yet. These extend `AbstractResourceTest` /
`AbstractBrainServiceIntegrationTest` / `AbstractAuditTest` / `AbstractResourceAuthzTest`
and each boots its own `TestCLMServer` **per class**, so they can't share a reused server.

Isolating them here keeps `insight-brain-service`'s own test run boot-free and lets this
boot-heavy set run as its own parallelizable CI lane (`reuseForks=false`, `forkCount=4` —
a fresh JVM per class). This is a migration staging area: as tests are converted they move
out to `-h2` / `-pg` / `-component-*`, and this module should shrink over time.

## What tests belong here

Any full-server test that can't (yet) use the reused-server model: server-restart tests,
`@Bean @Primary` overrides, `@ManualIqServerInit`, per-test SSL/FIPS, test-only `@Path`
poison-pills — plus anything simply too slow to have been converted so far. Tests move in
**unmodified** (they still extend the service test-jar base classes).

## Run

```bash
mvn verify -pl insight-brain-variant-test-legacy -Dskip-functional-test
```
