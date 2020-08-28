# ADR 5. Native Binaries for Nexus IQ CLI

Date: 2020-08-17

## Status

Proposed

## Context

For Innovation Week Q2 2020 a group looked at if it was possible to use the [GraalVM](https://www.graalvm.org/) `native-image` tool to create OS dependant native binaries of the Nexus IQ CLI jar.
The result of that week was very successful as we created working binaries for Linux, Mac, and Windows.
It was left as a POC until mid-August 2020 when it was officially launched as an initiative for the Integrations Dorado team. 

The need for native binaries ultimately stems from non-JVM customers not wanting to install JVM tooling in order to use IQ.
Specifically the requirement for them to download and install a 65-85mb JRE.

Notable technological challenges that remained after innovation week:
- Graal did not like Guice and Sisu due to their dynamic nature.
- Only tested with a non-obfuscated jar.
- A `native-image` requirement is that all classes are known up front. If it isn't built into the image it will throw an exception when accessed. Getting all the right values baked into the image can be difficult. We'll require a way to automate that. They provide tooling to accomplish this, but it makes the build process non-trivial. A large test suite would be necessary to cover all cases.
- The build would require a custom JDK (GraalVM).
- The build would also require a Windows and Mac agent to build those binaries.
  - It is notable that AWS (where our Jenkins runs) does not support Mac hosts.
  - It is also notable that we do not intend to target ARM at this time.

## Decisions

- We will ensure there is support for an obfuscated jar.
- We will decouple the image generation from the regular build using Maven modules and profiles since native image generation requires a non-trivial amount of time to build, and a custom JVM.
- We will run the native image generation as part of the 'Run Downstream' jenkins stage.
- We will aim to not increase the overall Jenkins build time by running the native image generate step in parallel with the 'Extra Tests' step. Current 'Extra Tests' time is 30 minutes and we'll come in well under that.
- We will reuse (and possibly expand) the existing IQ CLI test cases which will ensure all needed classes and resources will be loaded into the native images.
- We will work with ci/ops to make the GraalVM available to Jenkins.
- We will work with ci/ops to create a workable Jenkins agent for Windows.
- We will work with ci/ops to create a workable solution for Mac (as AWS does not have a Mac option).
- We will publish the native binaries to help.sonatype.com alongside the existing jar, as well as identify and publish to common package managers. (Likely deb and rpm ecosystems. Other candidates are homebrew and winget for Mac & Windows.)

## Consequences

- Native binaries available for Linux, Mac, and Windows to satisfy non-JVM IQ customers.
- These binaries are easily installable by IQ customers.
- IQ engineers are supported by a large test suite as well as a readme dedicated to the native image generation process and pitfalls.
- Parallelized build system that does not slow down the overall build.