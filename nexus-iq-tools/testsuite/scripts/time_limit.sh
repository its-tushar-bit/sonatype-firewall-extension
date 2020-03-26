#!/usr/bin/env bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

set -x;

TTL_MIN=${1:-60}

TTL_SEC=$((60*$TTL_MIN))

echo "auto shutdown after $TTL_SEC seconds"

sleep $TTL_SEC

shutdown -h now
