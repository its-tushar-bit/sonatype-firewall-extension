package com.sonatype.insight.brain;

import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

public class TestLicenseFingerprinter
    extends LicenseFingerprinter
{
  private String dummyLicenseFingerprint = "1234";

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
