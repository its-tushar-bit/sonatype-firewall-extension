#!/usr/bin/env bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

IQ_LICENSE=$(locate lifecycle | egrep "*.lic$" | sort | tail -n 1)
start=$(date +%s)

runtime=$((end-start))
if ./configure_test.py -iq ../../insight-brain-service/target/insight-brain-service-*-server.jar -tools ../target/nexus-iq-tools-*-SNAPSHOT.jar -auto ../target/nexus-iq-tools-*-automation.zip -lic $IQ_LICENSE ; then
   terraform init
   terraform apply -auto-approve;
   ./run_aws_test.sh;
   terraform destroy -auto-approve;
   CURRENT_TEST=$(ls -t | grep awsPerfRun | head -1)
   RESULT_FILE=$(tar tJf $CURRENT_TEST/results.tar.xz | grep perf_results)
   end=$(date +%s)
   runtime=$((end-start))
   echo -e "runtime: $runtime seconds\nresults:\n$(pwd)/$CURRENT_TEST/$RESULT_FILE";
else
    echo "not lucky";
fi
