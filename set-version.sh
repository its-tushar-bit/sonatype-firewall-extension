#!/bin/bash
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

dirname=`dirname $0`
dirname=`cd "$dirname" && pwd`
cd "$dirname"

# This will update all pom.xml files to use version specified as first parameter of this script

newVersion=$1
if [ -z "$newVersion" ]; then
    echo "usage: `basename $0` <new-version>"
    exit 1
fi

exec mvn org.codehaus.mojo:versions-maven-plugin:2.16.2:set \
    -DnewVersion="$newVersion" \
    -DgenerateBackupFiles=false \
    -DprocessAllModules=true
