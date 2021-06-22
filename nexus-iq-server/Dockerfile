#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

ARG SONATYPE_PRIVATE_REGISTRY

FROM $SONATYPE_PRIVATE_REGISTRY/ubi8/openjdk-8:1.3

# Build parameters
ARG IQ_SERVER_VERSION=1.118.0-01
ARG IQ_HOME="/opt/sonatype/nexus-iq-server"
ARG SONATYPE_WORK="/sonatype-work"
ARG CONFIG_HOME="/etc/nexus-iq-server"
ARG LOGS_HOME="/var/log/nexus-iq-server"
ARG TEMP="/tmp/work"

LABEL vendor=Sonatype \
  maintainer="Sonatype <support@sonatype.com>" \
  com.sonatype.name="Nexus IQ Server image"

USER root

# For testing
RUN microdnf update --nobest -y \
&& microdnf install -y procps \
&& microdnf clean all

# Create folders
RUN mkdir -m 0755 -p ${TEMP} \
&& mkdir -m 0755 -p ${IQ_HOME} \
&& mkdir -m 0755 -p ${SONATYPE_WORK} \
&& mkdir -m 0755 -p ${CONFIG_HOME} \
&& mkdir -m 0755 -p ${LOGS_HOME}

COPY . ${TEMP}

# Copy config.yml
COPY config.yml ${CONFIG_HOME}
RUN chmod 0644 ${CONFIG_HOME}/config.yml

# Unpack the server bundle and extract the server jar to the install directory
RUN mkdir -m 0755 ${TEMP}/iq-work \
&& tar -xvf ${TEMP}/target/nexus-iq-server-${IQ_SERVER_VERSION}-bundle.tar.gz -C ${TEMP}/iq-work \
&& mv ${TEMP}/iq-work/nexus-iq-server-${IQ_SERVER_VERSION}.jar ${IQ_HOME} \
&& cd ${IQ_HOME} \
&& rm -rf ${TEMP}

# Create start script
RUN echo "/usr/bin/java \${JAVA_OPTS} -jar nexus-iq-server-${IQ_SERVER_VERSION}.jar server ${CONFIG_HOME}/config.yml 2> ${LOGS_HOME}/stderr.log" > ${IQ_HOME}/start.sh \
&& chmod 0755 ${IQ_HOME}/start.sh

# Add group and user
RUN groupadd -g 1000 nexus \
&& adduser -u 1000 -d ${IQ_HOME} -c "Nexus IQ user" -g nexus -s /bin/false nexus \
&& cp /etc/passwd /home/jboss/passwd && chown jboss:root /home/jboss/passwd && chmod ug+rwX /home/jboss /home/jboss/passwd
# The above addition of cp-ing the /etc/password folder is required due to changes 
# to the OpenJDK8 image : https://access.redhat.com/articles/4859371

# Change owner to nexus user
RUN chown -R nexus:nexus ${IQ_HOME} \
&& chown -R nexus:nexus ${SONATYPE_WORK} \
&& chown -R nexus:nexus ${CONFIG_HOME} \
&& chown -R nexus:nexus ${LOGS_HOME}

# This is where we will store persistent data
VOLUME ${SONATYPE_WORK}
VOLUME ${LOGS_HOME}

# Expose the ports
EXPOSE 8070
EXPOSE 8071

# Change to nexus user
USER nexus

ENV JAVA_OPTS="-Djava.util.prefs.userRoot=${SONATYPE_WORK}/javaprefs"

WORKDIR ${IQ_HOME}

CMD [ "sh", "./start.sh" ]
