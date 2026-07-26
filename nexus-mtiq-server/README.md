<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# nexus-mtiq-server
MTIQ (Multi Tenant IQ) is a build of IQ server with an implementation of tennancy to support multiple tenants in Sonatype's SaaS offering.

## Docker builds
MTIQ is deployed to AWS ECS, so we package the application as a docker image.

The image is **base-less** (`FROM scratch`), following the DEVEX-2503 slim-image
paved path (the same strategy Repo Cloud and Firewall Pro use). The `Dockerfile`
builder stage assembles a minimal busybox + musl userland and the JRE
(`openjdk25-jre-headless`) into a staged rootfs via `apk add --root`, so the apk
package database ships in the final image and Sonatype's container scanner can
identify every OS package **and the JRE** (a hand-built jlink runtime would be
invisible to the scanner). The application is the shaded `nexus-mtiq-server.jar`
uber jar, run directly with `java -jar` — there is no bundled JDK and no
jreleaser/jlink step.

The image inputs are ordinary module build outputs in `nexus-mtiq-server/target/`
(`nexus-mtiq-server.jar`, `otel-java-agent.jar`, `jvm.options`), all produced by a
normal `mvn install`.

### Build the app
From the root of the `insight-brain` repository:
```shell
# builds nexus-mtiq-server.jar (uber jar) + copies the OTel agent and jvm.options into target/
mvnd clean install -Pquick -pl :nexus-mtiq-server -am
```

### Build for multiple platforms
```shell
docker buildx create --use
docker buildx build \
  --platform=linux/amd64,linux/arm64 \
  --tag sonatype.repo.sonatype.app/docker-all/mtiq/server:local nexus-mtiq-server
```

### Build for your system
```shell
docker build \
  --tag sonatype.repo.sonatype.app/docker-all/mtiq/server:local nexus-mtiq-server
```

> The builder-stage base is digest-pinned (`alpine:3.23@sha256:…`) in the
> `Dockerfile`, for reproducibility. Alpine **>= 3.23** is required (first release
> carrying `openjdk25`); bump the tag and digest together when updating.

### Running the MTIQ image locally
Coming soon on k8s!

In the meantime, follow this instructions: https://sonatype.atlassian.net/wiki/x/IYEFEQ
