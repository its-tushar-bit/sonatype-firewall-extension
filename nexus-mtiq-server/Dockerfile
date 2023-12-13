#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#
# MTIQ Docker image:
# - To build locally: docker build --tag docker-all.repo.sonatype.com/mtiq/server .
# - To run locally: docker run -it --rm --name mtiq-docker \
#   --user $(id -u):$(id -g) \  # So files created in the image are owned by your user
#   --net collin-local-dev \    # Optional. Put on the same Docker network as your postgres
#   -v /path/to/your/config-mtiq-docker.yml:/etc/nexus-iq-server/config.yaml
#   -v $HOME/dev/work/bin/sonatype-work:/sonatype-work \
#   docker-all.repo.sonatype.com/mtiq:latest

ARG SONATYPE_PRIVATE_REGISTRY=docker-all.repo.sonatype.com

FROM $SONATYPE_PRIVATE_REGISTRY/eclipse-temurin:11-jre

LABEL vendor=Sonatype \
  maintainer="MTIQ Team <mtiq-dev@sonatype.com>" \
  com.sonatype.name="MTIQ Server Docker Image"

RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

# Copy JAR
COPY target/nexus-mtiq-server.jar /opt/sonatype/nexus-iq-server/nexus-mtiq-server.jar

# Copy Datadog jar
COPY target/dd-java-agent.jar /opt/datadog/dd-java-agent.jar

# This is where we will store persistent data
VOLUME /sonatype-work

# Expose the ports
EXPOSE 8070
EXPOSE 8071

WORKDIR /opt/sonatype/nexus-iq-server

ENTRYPOINT ["java", \
             "-Djava.util.prefs.userRoot=/sonatype-work/javaprefs", \
             "-javaagent:/opt/datadog/dd-java-agent.jar", \
             "-XX:+AlwaysPreTouch", \
             "-XX:+CrashOnOutOfMemoryError", \
             "-XshowSettings:vm", \
             "-jar"]

CMD ["/opt/sonatype/nexus-iq-server/nexus-mtiq-server.jar", "server", "/etc/mtiq-server/config.yaml"]
