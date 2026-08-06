# insight-brain-variant-test-pg

IQ-on-PostgreSQL deployment-variant integration tests.

## Intent

Postgres counterpart of `insight-brain-variant-test-h2`. Boots `TestCLMServer` once
against an embedded PostgreSQL (zonky) cluster and reuses it across every test in the
module (`reuseForks=true`, `forkCount=1`). Kept in a separate module from the H2 variant
so each module uses exactly one `DatabaseContainerRule` fixture type.

## What tests belong here

Full-server, HTTP-level IQ tests converted to the `@IqPostgresTest` reused-server
pattern. Two kinds of tests land here:

- Tests that genuinely need Postgres semantics.
- H2 "deadlockers" — data-heavy tests that hang on H2 table locks under the reused
  server; Postgres MVCC avoids the deadlock, so they are re-targeted to `@IqPostgresTest`.

Same reused-server rules as the H2 module: no `@DirtiesContext`, no `@MockBean`.

## Run

```bash
mvn verify -pl insight-brain-variant-test-pg -Dskip-functional-test
```
