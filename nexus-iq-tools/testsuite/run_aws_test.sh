#!/usr/bin/env bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

set -e
set -x

source run_aws_test.config
EXTRA_PARAMETERS=""
if $USE_POSTGRES; then
  EXTRA_PARAMETERS="--use-postgres"
fi
if $MIGRATE_H2_TO_POSTGRES; then
  EXTRA_PARAMETERS="$EXTRA_PARAMETERS --migrate-h2-to-postgres"
fi

CURRENT_TEST=$(ls -t | grep awsPerfRun | head -1)

./scripts/dirupload.sh $CURRENT_TEST;
./scripts/run_remote.sh "cd /iqperf_eval/data/; ./test_execute.sh $EXTRA_PARAMETERS";
./scripts/fetch_results.sh $CURRENT_TEST;

cd $CURRENT_TEST;

tar xJf results.tar.xz;

RESULT_FILE=$(tar tJf results.tar.xz | grep perf_results)

cd ..;

echo "results available in $(pwd)/$CURRENT_TEST/$RESULT_FILE";
