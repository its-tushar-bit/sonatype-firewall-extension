#!/usr/bin/env bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

USE_POSTGRES=$1

yum install -y java-1.8.0
yum install -y python3
pip3 install pipenv
yum install -y emacs-nox

if $USE_POSTGRES; then
  yum install -y docker
  yum install -y postgresql
  service docker start
  mkdir /iqperf_eval/data/postgres_data
  docker run --name postgres-10.7 -e POSTGRES_PASSWORD=postgres \
      -v /iqperf_eval/data/postgres_data:/var/lib/postgresql/data -d -p 5432:5432 postgres:10.7
else
  echo 'Postgres in not installed';
fi
