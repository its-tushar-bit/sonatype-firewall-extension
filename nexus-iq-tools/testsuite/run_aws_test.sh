#!/usr/bin/env bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

set -x

source run_aws_test.config

CURRENT_TEST=$(ls -t | grep awsPerfRun | head -1)

./scripts/dirupload.sh $CURRENT_TEST;
./scripts/run_remote.sh "cd /iqperf_eval/data/; ./test_execute.sh $USE_POSTGRES $MIGRATE_H2_TO_POSTGRES";
./scripts/fetch_results.sh $CURRENT_TEST;

cd $CURRENT_TEST;

tar xJf results.tar.xz;

RESULT_FILE=$(tar tJf results.tar.xz | grep perf_results)

cd ..;

echo "results available in $(pwd)/$CURRENT_TEST/$RESULT_FILE";
