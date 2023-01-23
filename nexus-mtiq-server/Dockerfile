#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#
# MTIQ Docker image:
# - To build locally: docker build --tag docker-all.repo.sonatype.com/mtiq/snapshot .
# - To run locally: docker run -it --rm --name mtiq-docker \
#   --user $(id -u):$(id -g) \  # So files created in the image are owned by your user
#   --net collin-local-dev \    # Optional. Put on the same Docker network as your postgres
#   -v /path/to/your/config-mtiq-docker.yml:/etc/nexus-iq-server/config.yaml
#   -v $HOME/dev/work/bin/sonatype-work:/sonatype-work \
#   docker-all.repo.sonatype.com/mtiq/snapshot:latest

ARG SONATYPE_PRIVATE_REGISTRY=docker-all.repo.sonatype.com

# Get the Datadog Agent for APM
FROM $SONATYPE_PRIVATE_REGISTRY/alpine/curl as builder
RUN curl -L -o /dd-java-agent.jar https://github.com/DataDog/dd-trace-java/releases/latest/download/dd-java-agent.jar


# TODO: RSO currently doesn't proxy gcr.io
#FROM $SONATYPE_PRIVATE_REGISTRY/distroless/java11-debian11
FROM gcr.io/distroless/java11-debian11

LABEL vendor=Sonatype \
  maintainer="MTIQ Team <mtiq-dev@sonatype.com>" \
  com.sonatype.name="Nexus MTIQ Server Docker Image"

# Copy JAR
COPY target/nexus-mtiq-server.jar /opt/sonatype/nexus-iq-server/nexus-mtiq-server.jar

# Copy Datadog jar
COPY --from=builder /dd-java-agent.jar /opt/datadog/dd-java-agent.jar

# This is where we will store persistent data
VOLUME /sonatype-work

# Expose the ports
EXPOSE 8070
EXPOSE 8071

WORKDIR /opt/sonatype/nexus-iq-server

ENTRYPOINT ["/usr/bin/java", "-Djava.util.prefs.userRoot=/sonatype-work/javaprefs", "-javaagent:/opt/datadog/dd-java-agent.jar", "-jar"]
CMD ["/opt/sonatype/nexus-iq-server/nexus-mtiq-server.jar", "server", "/etc/nexus-iq-server/config.yaml"]
