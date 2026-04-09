@REM
@REM Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
@REM Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
@REM "Sonatype" is a trademark of Sonatype, Inc.
@REM

java @jvm.options -jar ${clm.server.jar} server config.yml 2> stderr.log
