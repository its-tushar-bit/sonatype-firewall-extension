<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# nexus-mtiq-server
MTIQ (Multi Tenant IQ) is a build of IQ server with an implementation of tennancy to support multiple tenants in Sonatype's SaaS offering.

## Docker builds
MTIQ runs in cloudy (k8s) so we package the application as a docker image. You can build docker images for your system locally, or for
multiple platforms. The MTIQ docker images require the jreleaser assemblies, which is the MTIQ application JAR bundled with the JDK. Because
this package isn't quick to build, it's not created for any standard maven goals.

CI builds this image and evaluates it against
Sonatype IQ policy as the `docker-nexus-iq-server-mtiq` application (`Jenkinsfile.main`,
`buildMtiqScanImage()` / `evaluateMtiqImagePolicy()`). The IQ stage action is the only gate: a policy
set to `Fail` blocks the build and publishes nothing, while one left at `Warn` is reported and the
image still publishes to RSC and ECR and promotes to shared-dev (per CLM-44494 the evaluation does not
mark the build UNSTABLE). The evaluation covers the Ubuntu OS packages read from the image's dpkg
database and the Java libraries the assembly ships as discrete jars; the JRE itself is a jlink runtime
the scanner cannot enumerate.

### Build the assemblies
From the root of the `insight-brain` repository
```shell
# build project (or use your other favorite build command)
mvnd clean install -Pquick
# downloads and installs the jdks used for the assemblies and places in the root target directory
mvn -pl :insight-brain -Pjdks
# packages the assemblies in nexus-mtiq-server/target/jreleaser/assemble/nexus-mtiq-server
mvn -pl :nexus-mtiq-server jreleaser:assemble
```

### Build for both multiple platforms
```shell
docker buildx create --use
docker buildx build \
  --platform=linux/amd64,linux/arm64
  --build-arg IQ_SERVER_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version -q -DforceStdout) \
  --tag sonatype.repo.sonatype.app/docker-all/mtiq/server:local .
```

### Build for your system
```shell
docker build \
  --build-arg IQ_SERVER_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version -q -DforceStdout) \
  --tag sonatype.repo.sonatype.app/docker-all/mtiq/server:local .
```

### Running the MTIQ image locally
Coming soon on k8s!

In the meantime, follow this instructions: https://sonatype.atlassian.net/wiki/x/IYEFEQ
