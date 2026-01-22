/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

@Named
@Singleton
public class TestLicenseFingerprinter
    extends LicenseFingerprinter
{
  private volatile String dummyLicenseFingerprint = "1234";

  @Override
  public String calculate() {
    return dummyLicenseFingerprint;
  }

  @Override
  public String calculate(ProductLicenseKey key) {
    return calculate();
  }

  public void setDummyLicenseFingerprint(String licenseFingerprint) {
    this.dummyLicenseFingerprint = licenseFingerprint;
  }
}
