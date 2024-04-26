<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# ReferenceLicenseUpdater

The files `license.sql`, `multi_license.sql` and `multi_license_license.sql` are what IQ actually uses to initialize
the license data in the db when it is installed. They are refreshed from HDS after startup but the data we use for
tests is not refreshed online and has to be maintained every now and then. To do this, run `ReferenceLicenseUpdater`
and commit the files.

Related PR can be found in: https://github.com/sonatype/insight-brain/pull/11348
