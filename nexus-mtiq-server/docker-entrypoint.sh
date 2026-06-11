#!/bin/bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

# Docker entrypoint script for MTIQ server with APM agent selection.
# Reads APM_AGENT environment variable to determine which Java agent to use:
#   - "datadog" (default): Use Datadog agent (already in JAVA_OPTS from Dockerfile)
#   - "otel": Use OpenTelemetry agent
#   - "none": No APM agent
#
# Only one agent runs at a time — running both simultaneously is not supported.
# In production, Terraform enforces mutual exclusivity (enable_observe vs enable_datadog).
#
# Note: The Datadog agent is included in JAVA_OPTS by default in the Dockerfile
# so that containers with overridden entry points (e.g. ECS command overrides)
# still load Datadog without requiring docker-entrypoint.sh to run.

set -e

APM_AGENT="${APM_AGENT:-datadog}"
JAVA_OPTS="${JAVA_OPTS:-}"

case "${APM_AGENT}" in
  datadog)
    echo "Using Datadog APM agent"
    # Datadog agent is already in JAVA_OPTS from Dockerfile ENV; nothing to do
    ;;
  otel)
    echo "Using OpenTelemetry APM agent"
    # Remove Datadog agent and add OTel agent.
    # This pattern must match the exact -javaagent string in the Dockerfile JAVA_OPTS ENV.
    if [[ "${JAVA_OPTS}" != *"-javaagent:/opt/datadog/dd-java-agent.jar"* ]]; then
      echo "WARNING: APM_AGENT=otel but Datadog agent flag not found in JAVA_OPTS (may have been overridden externally). OTel agent will be prepended but Datadog removal is a no-op."
    fi
    JAVA_OPTS="${JAVA_OPTS//-javaagent:\/opt\/datadog\/dd-java-agent.jar/}"
    JAVA_OPTS="-javaagent:/opt/otel/otel-java-agent.jar ${JAVA_OPTS}"
    ;;
  none)
    echo "APM agent disabled (APM_AGENT=none)"
    # Remove Datadog agent
    JAVA_OPTS="${JAVA_OPTS//-javaagent:\/opt\/datadog\/dd-java-agent.jar/}"
    ;;
  *)
    echo "Unknown APM_AGENT value: ${APM_AGENT}. Valid values: datadog, otel, none"
    exit 1
    ;;
esac

export JAVA_OPTS

# Execute the main entrypoint
exec "$@"
