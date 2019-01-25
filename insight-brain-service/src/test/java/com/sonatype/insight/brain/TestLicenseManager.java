/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import org.sonatype.licensing.product.util.LicenseFingerprinter;

@Named
@Singleton
public class TestLicenseManager
    extends CLMLicenseManager
{
  private final TestProductLicenseManager licenseManager;

  @Inject
  public TestLicenseManager(TestProductLicenseManager licenseManager,
                            LicenseFingerprinter licenseFingerprinter,
                            AuditRecorder auditRecorder)
  {
    super(licenseManager, licenseFingerprinter, auditRecorder);
    this.licenseManager = licenseManager;
  }

  @Override
  public Set<CLMEnforcementPoint> getEnforcementPoints() {
    // enforcement points are normally derived based on the products
    // for precise testing, we allow them to be manually overridden to a specific set
    Set<CLMEnforcementPoint> enforcementPoints = licenseManager.getEnforcementPoints();
    if (enforcementPoints != null) {
      return enforcementPoints;
    }
    return super.getEnforcementPoints();
  }
}
