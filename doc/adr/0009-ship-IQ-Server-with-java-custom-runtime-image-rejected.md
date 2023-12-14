# ADR 9. Ship IQ Server with a Java Custom Runtime Image

Date: 2021-11-23

## Status

Rejected.
Using a Docker image will be explored instead.

## Context

Currently we're providing IQ Server to on-prem customers using a JAR file which can be run in Java 8 or 11.

That approach requires on-prem customers to have installed a Java 8 JDK/JRE or the Java 11 JDK ([there is no JRE for Java 11](https://www.oracle.com/java/technologies/javase/11-relnote-issues.html)) in the server running IQ Server.

Since we still have customers running IQ Server with Java 8 our codebase needs to be compatible with that version, which causes problems including:

- IQ Server having to stick on some dependencies that cannot be updated anymore as newer versions don't support Java 8.
- Our codebase not being able to benefit from improvements in newer versions of Java.
- Reaching the "end of life" for Java 8 ([Nov 2026 for OpenJDK](https://adoptium.net/es/support/#_release_roadmap)).

## Decision

We should follow the recommendation given by Oracle at https://www.oracle.com/java/technologies/javase/11-relnote-issues.html:

> the JRE or Server JRE is no longer offered. Only the JDK is offered. Users can use jlink to create smaller custom runtimes.

Based on an initial work done during an Improvement Day, it seems possible to create such a custom runtime image for IQ Server:

- Video (last demo): https://sonatype.atlassian.net/wiki/spaces/LRN/pages/208044033/Improvement+Day+-+Session+156+-+October+26+2023
- Deck: https://docs.google.com/presentation/d/11pJizZpgoG4DpOfits46FpYbveqouI_LTKBpceUTKzs/edit?usp=sharing

Initially, we'd want to create that image for Java 11, since that's a supported version for IQ Server.

## Consequences

- We'd need to further test all features in IQ Server to make sure they all work well when using a custom runtime image.
  - A decision should be made on whether to split those tests across all/some teams or allocate all the effort on one team dedicated to this.
- Similarly to how the JDK is packaged and distributed per OS and CPU architecture, we'd have to build a custom runtime image the same way.
- On-prem customers could run IQ Server on their environments:
  - Without having to upgrade their existing Java version.
  - Without having to install Java if not already installed.
- Our codebase could eventually be changed to use newer versions of Java without having to disrupt on-prem customers.
