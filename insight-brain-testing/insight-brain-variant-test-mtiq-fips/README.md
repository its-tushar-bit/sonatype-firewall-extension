# insight-brain-variant-test-mtiq-fips

FIPS-mode **multi-tenant** (MTIQ) integration tests.

## Intent

Run the same `MultiTenant*` integration scenarios as `nexus-mtiq-server`, but with a BouncyCastle FIPS
security provider installed (`FipsTestUtil.insertBouncyCastleFipsProvider`) and `FIPS_MODE_ENABLED` set.

Because installing a FIPS provider mutates JVM-global `java.security.Security`, these tests **cannot**
share a reused single-JVM context — they would poison every other test in that JVM. So this module runs
`reuseForks=false`, giving each FIPS test class its own clean JVM (and its own security-provider state).

## Why a separate module from `insight-brain-variant-test-fips`

The MTIQ app and the plain IQ app both define Spring `@Configuration` classes in the
`com.sonatype.insight.brain.service` package, so they **cannot share a test classpath**: the single-tenant
IQ server would component-scan the MTIQ beans (e.g. `multiTenantProductLicense`,
`multiTenantEncryptionKeyStore`, `multiTenantTaskScheduler`) and fail to boot with duplicate/`@Primary`
bean conflicts. This is the same reason `nexus-mtiq-server` / `insight-brain-variant-test-mtiq` are kept
separate from the IQ variant modules. So the single-tenant IQ FIPS tests live in
`insight-brain-variant-test-fips` and the multi-tenant FIPS tests live here.

## What tests belong here

The `MultiTenant*FIPSTest` subclasses of the MTIQ integration tests. The non-FIPS base classes stay in
`nexus-mtiq-server`; they arrive here via the `nexus-mtiq-server` test-jar purely as superclasses and are
excluded from standalone execution by the `**/*FIPSTest.*` failsafe include.

## Run

```bash
mvn verify -pl insight-brain-variant-test-mtiq-fips -Dskip-functional-test
```
