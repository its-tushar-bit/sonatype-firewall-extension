# insight-brain-variant-test-component-h2

Spring-context + H2 component integration tests.

## Intent

Migrate the JUnit 4 `AbstractComponentTest` / `AbstractServiceAuthzTest` chain to the
JUnit 5 reused-context pattern. `SpringExtension` caches ONE `ApplicationContext` (and
one in-memory **H2** fixture) across every class in the module, and
`ComponentTestDbHarnessExtension` reproduces the ordered
`DatabaseContainerRule`/`SearchIndexRule`/`TemporaryEntity`/Mockito lifecycle under
Jupiter. `reuseForks=true` shares the context/fixture across the whole module JVM.

These are **component-level** tests (service beans against a real DB + Spring context),
not full HTTP server boots — that distinguishes this module from `-h2` / `-pg`.

## What tests belong here

Converted `*Test` classes that keep `extends AbstractComponentTest` (for the helper
surface) and add `@ComponentH2Test`: Service / ServiceAuthz / Converter / Info / etc.
tests that only need a Spring context + H2. Postgres-only or H2-deadlocking component
tests go to `insight-brain-variant-test-component-pg`; FIPS variants go to
`insight-brain-variant-test-fips`.

## Run

```bash
mvn verify -pl insight-brain-variant-test-component-h2 -Dskip-functional-test
```
