<!--

    Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

Insight Brain
=============

The on-premises server that customers run to evaluate policy against applications and review the results.

Building
========

Standard Maven build.

Additional PermGen needs to be allocated to avoid a failing build during compilation.  `MAVEN_OPTS="-XX:MaxPermSize=128M"` has been successfully used.
