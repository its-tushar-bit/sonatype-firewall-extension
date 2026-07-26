#!/bin/sh
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

# Launcher shim for the base-less (scratch) MTIQ image. The apk-provided JRE runs
# the shaded uber jar directly, so this script exists only to assemble the java
# command from JAVA_OPTS (the OpenTelemetry/Observe agent and JVM flags, set in
# the Dockerfile) and the repo-root jvm.options argfile, then exec java as the
# tini-supervised child. Override JAVA_OPTS (e.g. via ECS env) to change JVM
# flags or drop the OTel -javaagent.
#
# POSIX sh (Alpine busybox ash): no bashisms.

set -e

APP_HOME="${APP_HOME:-/opt/sonatype/nexus-iq-server}"
JAVA_OPTS="${JAVA_OPTS:-}"

# JAVA_OPTS is intentionally unquoted so its flags word-split into arguments;
# jvm.options (repo-root single source of truth for the add-opens /
# enable-native-access flags) is passed as a JVM @argfile. "$@" is the
# sub-command (default: server <config>).
exec java ${JAVA_OPTS} @"${APP_HOME}/jvm.options" -jar "${APP_HOME}/nexus-mtiq-server.jar" "$@"
