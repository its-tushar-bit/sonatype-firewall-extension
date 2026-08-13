# insight-brain-variant-test-mtiq

MTIQ (multi-tenant IQ) deployment-variant integration tests.

## Intent

Boot `MultiTenantInsightBrainService` once via the `@MtiqTest` meta-annotation and reuse
it across the module, exercising endpoints under the global tenant. Kept as a SEPARATE
module from the IQ variant tests because the MTIQ app and the plain IQ app both define
Spring `@Configuration` classes in the `com.sonatype.insight.brain.service` package and
cannot share a test classpath (the IQ server would component-scan
`MtiqConfigurationAliases` and fail to boot). Shared machinery lives in
`insight-brain-variant-test-support`.

## What tests belong here

Multi-tenant server tests using `@MtiqTest` / `MtiqTestContext` (per-test tenant
provisioning + JWT admin auth + tenant-scoped `tempEntity`/license). Any test whose
behavior is specific to the MTIQ bean graph or tenant isolation belongs here rather than
in the single-tenant IQ variant modules.

## Run

```bash
mvn verify -pl insight-brain-variant-test-mtiq -Dskip-functional-test
```
