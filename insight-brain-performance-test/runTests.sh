#!/bin/bash
#
# Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

mvn test
mvn test -P static-query-no-leading-wildcards
mvn test -P dynamic-query
mvn test -P dynamic-query-group-search
mvn test -P static-login
mvn test -P dynamic-login
