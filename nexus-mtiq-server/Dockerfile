#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#
# MTIQ Docker image:
# - To build locally: docker build --build-arg IQ_SERVER_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version -q -DforceStdout) --tag sonatype.repo.sonatype.app/docker-all/mtiq/server .
# - To run locally: docker run -it --rm --name mtiq-docker \
#   --user $(id -u):$(id -g) \  # So files created in the image are owned by your user
#   --net collin-local-dev \    # Optional. Put on the same Docker network as your postgres
#   -v /path/to/your/config-mtiq-docker.yml:/etc/nexus-iq-server/config.yaml
#   -v $HOME/dev/work/bin/sonatype-work:/sonatype-work \
#   sonatype.repo.sonatype.app/docker-all/mtiq:latest
ARG SONATYPE_PRIVATE_REGISTRY=sonatype.repo.sonatype.app/docker-all

FROM $SONATYPE_PRIVATE_REGISTRY/ubuntu:24.04 AS builder
ARG TARGETPLATFORM
ARG IQ_SERVER_VERSION

# Copy both linux architectures to /opt/sonatype
COPY target/jreleaser/assemble/nexus-mtiq-server/jlink/nexus-mtiq-server-${IQ_SERVER_VERSION}-linux-x86_64.tgz \
  target/jreleaser/assemble/nexus-mtiq-server/jlink/nexus-mtiq-server-${IQ_SERVER_VERSION}-linux-aarch_64.tgz \
  /opt/sonatype/

# Only extract the correct platform
RUN if [ "$TARGETPLATFORM" = "linux/amd64" ]; then \
      tar -xzf /opt/sonatype/nexus-mtiq-server-${IQ_SERVER_VERSION}-linux-x86_64.tgz -C /opt/sonatype/; \
    elif [ "$TARGETPLATFORM" = "linux/arm64" ]; then \
      tar -xzf /opt/sonatype/nexus-mtiq-server-${IQ_SERVER_VERSION}-linux-aarch_64.tgz -C /opt/sonatype/; \
    else \
      echo "Unsupported platform: $TARGETPLATFORM"; exit 1; \
    fi
RUN rm /opt/sonatype/nexus-mtiq-server-${IQ_SERVER_VERSION}-linux-*.tgz
RUN mv /opt/sonatype/nexus-mtiq-server-${IQ_SERVER_VERSION}-linux-* /opt/sonatype/nexus-iq-server

FROM $SONATYPE_PRIVATE_REGISTRY/ubuntu:24.04

LABEL vendor=Sonatype \
  maintainer="MTIQ Team <mtiq-dev@sonatype.com>" \
  com.sonatype.name="MTIQ Server Docker Image"

RUN apt-get update && apt-get install -y \
  git \
  curl \
  libjemalloc2 \
  tini \
&& rm -rf /var/lib/apt/lists/*

# Copy assembly from builder
COPY --from=builder /opt/sonatype/nexus-iq-server /opt/sonatype/nexus-iq-server

# Copy telemetry jars (Datadog and OpenTelemetry)
COPY target/dd-java-agent.jar /opt/datadog/dd-java-agent.jar
COPY target/otel-java-agent.jar /opt/otel/otel-java-agent.jar

# This is where we will store persistent data
VOLUME /sonatype-work

# Expose the ports
EXPOSE 8070
EXPOSE 8071

WORKDIR /opt/sonatype/nexus-iq-server

ENV JAVA_OPTS="-javaagent:/opt/datadog/dd-java-agent.jar -Dgit.without.shell=true -Djava.util.prefs.userRoot=/sonatype-work/javaprefs -XX:+AlwaysPreTouch -XX:+CrashOnOutOfMemoryError -XshowSettings:vm"
ENV LD_PRELOAD="libjemalloc.so.2"

# Copy entrypoint script for APM agent selection
COPY docker-entrypoint.sh /opt/sonatype/nexus-iq-server/bin/docker-entrypoint.sh
RUN chmod +x /opt/sonatype/nexus-iq-server/bin/docker-entrypoint.sh

# Use tini init in order to get proper orphaned process reaping
# Entry point script handles APM agent selection before executing the server
ENTRYPOINT ["tini", "--", "/opt/sonatype/nexus-iq-server/bin/docker-entrypoint.sh", "/opt/sonatype/nexus-iq-server/bin/nexus-mtiq-server"]

CMD ["server", "/etc/mtiq-server/config.yaml"]
