#!/bin/sh
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

#
# Quick-builds insight-brain-service and all of its dependencies (including insight-brain-frontend).
# This is enough to build what you would typically run locally, and what the functional tests need.
# It does not however build the extra MTIQ modules or the customer-ready distributions.
#
# Usage: ./local-dev-build.sh
exec mvn clean install -Pquick -pl :insight-brain-service -am
