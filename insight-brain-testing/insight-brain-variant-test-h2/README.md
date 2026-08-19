# insight-brain-variant-test-h2

IQ-on-H2 deployment-variant integration tests.

## Intent

Boot the real `TestCLMServer` once against an embedded **H2** database and reuse that
running server across every test in the module (`reuseForks=true`, `forkCount=1`),
amortizing the ~9s boot instead of paying it per class. Split from the Postgres variant
so each module uses exactly one `DatabaseContainerRule` fixture type and can safely
reuse forks.

## What tests belong here

Full-server, HTTP-level tests that exercise real IQ endpoints (Resource / V2 / Filter /
Servlet / Service / updater tests) converted to the `@IqH2Test` reused-server pattern.
This is the default home for converted server-booting tests. Route a test to
`insight-brain-variant-test-pg` instead only when it deadlocks on H2 table locks.

Rules for tests in this module: no `@DirtiesContext`, no `@MockBean` — either breaks the
reused-server cache key. Tests needing runtime-config/auth fixtures the shared harness
can't provide (SAML signing, reverse-proxy baseUrl, ...) are quarantined via failsafe
excludes and belong in the escalate/legacy lane.

## Run

```bash
mvn verify -pl insight-brain-variant-test-h2 -Dskip-functional-test
```
