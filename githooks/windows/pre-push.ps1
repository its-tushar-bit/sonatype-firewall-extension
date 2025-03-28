#!powershell
#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#
Write-Host 'pre-push hook - running'
mvn -e -C -N validate -P pre-check
Write-Host 'pre-push hook - completed. see insight-brain/docs/devdocs/git-hooks.md for details'
exit $LASTEXITCODE
