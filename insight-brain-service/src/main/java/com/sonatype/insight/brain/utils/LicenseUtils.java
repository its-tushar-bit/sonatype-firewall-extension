/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService.LicenseWithThreatLevel;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.License;

/**
 * @since 1.12
 */
public class LicenseUtils
{
  private static final LicenseDAO licenseDAO = new LicenseDAO();

  private LicenseUtils() {
    // Utility class
  }

  public static LicenseWithThreatLevel getLicenseWithThreatLevel(final Owner owner, final License license) {
    LicenseWithThreatLevel licenseWithThreatLevel = new LicenseWithThreatLevel();
    licenseWithThreatLevel.license = new com.sonatype.clm.dto.model.License(license.getId(),
        license.getShortDisplayName());
    licenseWithThreatLevel.threatLevel =
        licenseDAO.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(owner, license.getId());
    return licenseWithThreatLevel;
  }
}
