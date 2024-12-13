#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#
FROM registry.access.redhat.com/ubi9/openjdk-17 AS builder
ARG TEMP="/tmp/work"
ARG IQ_SERVER_VERSION

USER root
RUN microdnf update -y && microdnf install -y gzip tar
RUN mkdir -m 0755 -p ${TEMP}
COPY target/nexus-iq-server-${IQ_SERVER_VERSION}-bundle.tar.gz ${TEMP}
RUN cd ${TEMP} && tar -xzf nexus-iq-server-${IQ_SERVER_VERSION}-bundle.tar.gz


FROM registry.access.redhat.com/ubi9/openjdk-17

# Build parameters
ARG IQ_SERVER_VERSION
ARG IQ_HOME="/opt/sonatype/nexus-iq-server"
ARG SONATYPE_WORK="/sonatype-work"
ARG CONFIG_HOME="/etc/nexus-iq-server"
ARG LOGS_HOME="/var/log/nexus-iq-server"
ARG TEMP="/tmp/work"
ARG GID=1000
ARG UID=1000
ARG TIMEOUT=600

LABEL vendor=Sonatype \
  maintainer="Sonatype <support@sonatype.com>" \
  com.sonatype.name="Nexus IQ Server image"

USER root

# For testing
RUN microdnf update -y \
&& microdnf install -y procps gzip unzip tar shadow-utils findutils util-linux less rsync git \
&& microdnf clean all


# Create folders
RUN mkdir -m 0755 -p ${IQ_HOME} \
&& mkdir -m 0755 -p ${SONATYPE_WORK} \
&& mkdir -m 0755 -p ${CONFIG_HOME} \
&& mkdir -m 0755 -p ${LOGS_HOME}

# Add group and user
RUN groupadd -g ${GID} nexus \
&& adduser -u ${UID} -d ${IQ_HOME} -c "Nexus IQ user" -g nexus -s /bin/false nexus
#&& cp /etc/passwd /home/jboss/passwd && chown jboss:root /home/jboss/passwd && chmod ug+rwX /home/jboss /home/jboss/passwd
# The above addition of cp-ing the /etc/password folder is required due to changes
# to the OpenJDK8 image : https://access.redhat.com/articles/4859371

# Change owner to nexus user
RUN chown -R nexus:nexus ${IQ_HOME} \
&& chown -R nexus:nexus ${SONATYPE_WORK} \
&& chown -R nexus:nexus ${CONFIG_HOME} \
&& chown -R nexus:nexus ${LOGS_HOME}

# Copy config.yml
COPY --chown=nexus:nexus --chmod=0644 config.yml ${CONFIG_HOME}
COPY --chown=nexus:nexus --from=builder ${TEMP}/nexus-iq-server-${IQ_SERVER_VERSION}.jar ${IQ_HOME}

# Create start script
RUN echo "/usr/bin/java \${JAVA_OPTS} -jar nexus-iq-server-${IQ_SERVER_VERSION}.jar server ${CONFIG_HOME}/config.yml 2> ${LOGS_HOME}/stderr.log" > ${IQ_HOME}/start.sh \
&& chmod 0755 ${IQ_HOME}/start.sh

# This is where we will store persistent data
VOLUME ${SONATYPE_WORK}
VOLUME ${LOGS_HOME}

# Expose the ports
EXPOSE 8070
EXPOSE 8071

# Wire up health check
HEALTHCHECK CMD curl --fail --silent --show-error http://localhost:8071/healthcheck || exit 1

# Change to nexus user
USER nexus

ENV JAVA_OPTS="-Djava.util.prefs.userRoot=${SONATYPE_WORK}/javaprefs --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/sun.security.rsa=ALL-UNNAMED --add-opens=java.base/sun.security.x509=ALL-UNNAMED --add-opens=java.base/sun.security.util=ALL-UNNAMED --add-opens=java.xml/com.sun.org.apache.xerces.internal.jaxp.datatype=ALL-UNNAMED "
ENV SONATYPE_INTERNAL_HOST_SYSTEM=Docker

WORKDIR ${IQ_HOME}

CMD [ "sh", "./start.sh" ]
