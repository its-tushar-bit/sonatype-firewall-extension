/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

class DummyLicenseDataUpdater
    extends LicenseDataUpdater
{
  public DummyLicenseDataUpdater(final LicenseDAO licenseDAO, final MultiLicenseDAO multiLicenseDAO) {
    super(licenseDAO, multiLicenseDAO);
  }

  @Override
  public void doUpdate() {
  }
}
