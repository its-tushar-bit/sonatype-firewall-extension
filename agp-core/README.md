# agp-core

Framework-neutral core for Agent P: domain model, run lifecycle, recommendation selection and
onboarding coordination, behind ports that hosts implement.

## Two standing constraints

**This module targets Java 21; the rest of the reactor targets Java 25.** Its primary consumer is
Guide's `backend-server`, which runs Java 21, and a Java 21 toolchain cannot read Java 25 class
files. `maven.compiler.release=21` enforces it for this module's own code, `enforceBytecodeVersion`
enforces it for dependencies, and `animal-sniffer-maven-plugin` catches post-21 API usage that
slips past both. This is permanent, not a migration step.

**This module has no framework dependencies, and that is build-enforced.** Spring, JAX-RS, the AWS
SDK, Quartz, source-control SDKs, jOOQ and Redis clients are banned by `maven-enforcer-plugin`,
transitively and at every scope. If you need one, you need a port: declare the interface here and
implement it in the host. `insight-brain-common` is banned in effect too, because its compile-scope
`jakarta.ws.rs-api` would make the rule unsatisfiable.

## Why it does not inherit the reactor parent

It parents to `com.sonatype.buildsupport:private-parent` with `<relativePath/>` and carries its own
version line, so releases are independent of Nexus IQ's. The cost is no inherited
`dependencyManagement` — every dependency in `pom.xml` pins its own version explicitly.

## Testing

Tests use in-memory fakes only: no database, no queue, no web server. If a test here needs real
infrastructure, the seam is in the wrong place and the port needs reshaping.

## Verifying the dependency boundary is actually enforced

The `enforcer-negative-test` profile in `pom.xml` adds a banned dependency (`spring-context`) so
the `bannedDependencies` rule above can be exercised against the rule as configured, not a copy of
it. It is inert unless activated explicitly:

    mvn validate -pl agp-core -Penforcer-negative-test

Expected: `BUILD FAILURE`, quoting "agp-core must stay framework-neutral". A `BUILD SUCCESS` here
means the boundary is not being enforced — fix the rule, not the profile.

## animal-sniffer signature gap

The `animal-sniffer-maven-plugin` check pins `org.codehaus.mojo.signature:java18:1.0`, not
`java-21:1.0`, because `java-21:1.0` is not mirrored in this org's Maven repository (`mvn
dependency:get` against it fails to resolve). `java18` is the highest commonly mirrored signature
and still catches any API added between Java 18 and Java 21 that this module might
accidentally reference, though not APIs added between Java 18 and Java 21 that are actually valid
for a Java 21 target. The two enforcer rules above — `maven.compiler.release=21` and
`enforceBytecodeVersion` with `maxJdkVersion=21` — are the primary guards for the Java 21 boundary
and do not depend on this signature; animal-sniffer is a supplementary third layer, so the gap
narrows what it catches without removing the boundary itself.
