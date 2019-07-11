/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;

import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

@Named
@Singleton
public class TestLicenseManager
    extends CLMLicenseManager
{
  private final TestProductLicenseManager licenseManager;

  @Inject
  public TestLicenseManager(
      ProductLicense productLicense,
      TestProductLicenseManager licenseManager,
      LicenseFingerprinter licenseFingerprinter,
      AuditRecorder auditRecorder)
  {
    super(productLicense, licenseManager, licenseFingerprinter, auditRecorder);
    this.licenseManager = licenseManager;
  }

  private void reloadLicenseData() {
    try {
      installLicense(null);
    }
    catch (IOException | LicensingException e) {
      throw new RuntimeException(e);
    }
  }

  public void setFeatures(LicensedFeature... features) {
    licenseManager.setFeatures(features);
    reloadLicenseData();
  }

  public void setMissingFeatures(LicensedFeature feature, LicensedFeature... features) {
    licenseManager.setFeatures(EnumSet.complementOf(EnumSet.of(feature, features)).toArray(new LicensedFeature[0]));
    reloadLicenseData();
  }

  public void setStageTypes(StageType... stageTypes) {
    licenseManager.setStageTypes(stageTypes);
    reloadLicenseData();
  }

  public void setStageTypes(Collection<StageType> stageTypes) {
    setStageTypes(stageTypes.toArray(new StageType[stageTypes.size()]));
  }
}
