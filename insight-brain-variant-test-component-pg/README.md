# insight-brain-variant-test-component-pg

Spring-context + PostgreSQL component integration tests.

## Intent

Postgres counterpart of `insight-brain-variant-test-component-h2`. Same JUnit 5
reused-context migration off the `AbstractComponentTest` / `AbstractServiceAuthzTest`
chain, but `ComponentTestPgHarnessExtension` steers the JVM-wide `DatabaseContainerRule`
onto an embedded PostgreSQL (zonky) fixture. `SpringExtension` caches ONE
`ApplicationContext` and the ONE PG cluster is provisioned once (`reuseForks=true`).

## What tests belong here

Converted `*Test` classes that keep `extends AbstractComponentTest` and add
`@ComponentPgTest`:

- Component tests that need Postgres semantics.
- H2 deadlockers re-routed from `-component-h2` (H2 table-lock hangs under the reused
  context; PG MVCC avoids them).

## Run

```bash
mvn verify -pl insight-brain-variant-test-component-pg -Dskip-functional-test
```
