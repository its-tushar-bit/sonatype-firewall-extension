# insight-brain-variant-test-fips

FIPS-mode integration tests for the single-tenant IQ server.

## Intent

Run the single-tenant IQ `*FIPSTest` scenarios (moved out of `insight-brain-service`) with a
BouncyCastle FIPS security provider installed (`FipsTestUtil.insertBouncyCastleFipsProvider`) and
`FIPS_MODE_ENABLED` set.

Because installing a FIPS provider mutates JVM-global `java.security.Security`, these tests
**cannot** share a reused single-JVM context — they would poison every other test in that JVM. So
this module runs `reuseForks=false`, giving each FIPS test class its own clean JVM (and its own
security-provider state).

## What tests belong here

The single-tenant IQ `*FIPSTest` subclasses. Each still `extends` its non-FIPS base class, which
**stays in `insight-brain-service`** and arrives here on the classpath via the
`insight-brain-service` test-jar purely as a superclass. The failsafe include is narrowed to
`**/*FIPSTest.*`, so only the FIPS subclasses run and the non-FIPS bases never execute standalone.
(Multi-tenant FIPS tests live in the separate `insight-brain-variant-test-mtiq-fips` module.)

## Run

```bash
mvn verify -pl insight-brain-variant-test-fips -Dskip-functional-test
```
