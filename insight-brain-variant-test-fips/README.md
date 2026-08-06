# insight-brain-variant-test-fips

FIPS-mode component integration tests.

## Intent

Run the same `AbstractComponentTest` / `AbstractServiceAuthzTest` scenarios as
`insight-brain-variant-test-component-h2`, but with a BouncyCastle FIPS security provider
installed (`FipsTestUtil.insertBouncyCastleFipsProvider`) and `FIPS_MODE_ENABLED` set.

Because installing a FIPS provider mutates JVM-global `java.security.Security`, these
tests **cannot** share the reused single-JVM context of the component-h2 module — they
would poison every other test in that JVM. So this module runs `reuseForks=false`,
giving each FIPS test class its own clean JVM (and its own security-provider state).

## What tests belong here

FIPS subclasses of the component tests. The non-FIPS base classes are duplicated here
only as superclasses for those subclasses (their non-FIPS run stays in `-component-h2`)
and are excluded from standalone execution via failsafe excludes.

## Run

```bash
mvn verify -pl insight-brain-variant-test-fips -Dskip-functional-test
```
