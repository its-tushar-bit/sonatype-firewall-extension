/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.MultiLicense;

/**
 * Default implementation of {@link LicenseNameProvider} that uses {@link LicenseDAO} and {@link MultiLicenseDAO}
 * to get license display names.
 */
@Named
@Singleton
public class DefaultLicenseNameProvider
    implements LicenseNameProvider
{
  @Inject
  private LicenseDAO licenseDAO;

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  @Override
  public String getShortDisplayName(String licenseId, boolean isMultiLicense) {
    String result = licenseId;
    if (isMultiLicense) {
      MultiLicense multiLicense = multiLicenseDAO.getById(licenseId);
      if (multiLicense != null) {
        result = multiLicense.getShortDisplayName();
      }
    }
    else {
      License license = licenseDAO.getById(licenseId);
      if (license != null) {
        result = license.getShortDisplayName();
      }
    }
    return result;
  }
}
