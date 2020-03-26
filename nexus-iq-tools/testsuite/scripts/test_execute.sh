#!/usr/bin/env bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

set -x;

cd $(ls -t | grep aws | head -1)

unzip nexus-iq-tools-*-automation.zip

cd automation/

pipenv --python /usr/bin/python3 sync

pipenv run python run_performance_eval.py -p ../performance-test-profile-*.json -iq ../insight-brain-service-*.jar -tools ../nexus-iq-tools-*.jar -lic $(pwd)/../*.lic -k

cd perfTemp_*; tar cvJf ../../results.tar.xz ../../performance-test-profile-*.json ../*.out *.{log,out,txt,json}; cd ..;

echo $(pwd)/results.tar.xz
