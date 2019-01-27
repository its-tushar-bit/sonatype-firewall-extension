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
import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;

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
  public Set<Feature> getFeatures() {
    // features are normally derived based on the products
    // for precise testing, we allow them to be manually overridden to a specific set
    Set<Feature> features = licenseManager.getFeatures();
    if (features != null) {
      return features;
    }
    return super.getFeatures();
  }
}
