#!/bin/sh
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

#
# Runs the built insight-brain-service (does not actually build it, run local-dev-build.sh first)
# on port 8072 and runs the frontend watch mode on 8070, which proxies API calls to the backend at 8072.
# This gives you an overall running system accessible at port 8070, with hot-reloading of frontend
# code changes (though you must explicitly refresh the browser).
#
# Usage: ./local-dev-run.sh

# Start frontend in background (proxies API calls to port 8072)
(cd insight-brain-frontend && yarn start) &

# Start backend in foreground on port 8072
cd insight-brain-service && mvn exec:java \
    -Dexec.mainClass=com.sonatype.insight.brain.service.InsightBrainService \
    -Dexec.args="server src/test/resources/config-dev.yml" \
    -Ddw.server.applicationConnectors[0].port=8072

# If the backend process exits, wait here until the user Ctrl-Cs or the frontend dies.  Without this, backend crashes
# would cause the frontend to keep running as orphans owned by the init process (and not easily killed by Ctrl-C).
#
# Note: if the user presses Ctrl-C while the backend is still running, that sends SIGINT to both the backend and
# frontend (and this shell).  They are all in the same process group.
wait
