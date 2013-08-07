package com.sonatype.insight.brain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.Features;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.internal.DefaultLicenseKey;

import com.sonatype.insight.brain.product.license.CLMFeature;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

/**
 * Simple replacement for a ProductLicenseManager.
 */
public class TestProductLicenseManager
    implements ProductLicenseManager
{
  private boolean valid;

  private ProductLicenseKey key;

  private int appCount = 100;

  private Date expirationDate = new Date(System.currentTimeMillis() + 600 * 1000);

  private Set<CLMEnforcementPoint> enforcementPoints = new HashSet<CLMEnforcementPoint>();

  private boolean forceInstallFailure = false;

  public TestProductLicenseManager() {
    this(false);
  }

  public TestProductLicenseManager(boolean valid) {
    enforcementPoints.add(CLMEnforcementPoint.Build);
    enforcementPoints.add(CLMEnforcementPoint.Develop);
    enforcementPoints.add(CLMEnforcementPoint.Procure);
    enforcementPoints.add(CLMEnforcementPoint.Release);
    enforcementPoints.add(CLMEnforcementPoint.StageRelease);

    this.valid = valid;

    if (this.valid) {
      createKey();
    }
  }

  @Override
  public void installLicense(final InputStream licenseFile) throws IOException, LicensingException {
    if (forceInstallFailure) {
      throw new LicensingException("An error occurred");
    }

    valid = true;
    createKey();
  }

  private void createKey() {
    Map<String, Feature> featureMap = new HashMap<String, Feature>();
    featureMap.put(CLMFeature.ID, new CLMFeature());
    Properties properties = new Properties();

    StringBuffer sb = new StringBuffer();

    for (CLMEnforcementPoint ep : enforcementPoints) {
      sb.append(ep.name()).append(",");
    }

    if (sb.length() > 0) {
      sb.setLength(sb.length() - 1);
    }

    properties.put(ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS, sb.toString());
    properties.put(ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT, Integer.toString(appCount));
    key = new DefaultLicenseKey(new Features(featureMap));
    key.setEffectiveDate(new Date(System.currentTimeMillis() - 10000));
    key.setExpirationDate(expirationDate);
    key.setProperties(properties);
  }

  @Override
  public void uninstallLicense() throws LicensingException {
    valid = false;
    key = null;
  }

  @Override
  public ProductLicenseKey getLicenseDetails() throws LicensingException {
    if (!valid) {
      throw new LicensingException("Not licensed");
    }
    return key;
  }

  @Override
  public ProductLicenseKey getLicenseDetails(final InputStream licenseFile) throws IOException, LicensingException {
    if (!valid) {
      throw new LicensingException("Not licensed");
    }
    return key;
  }

  @Override
  public void verifyLicenseAndFeature(final Feature feature) throws LicensingException {
    // TODO
  }

  @Override
  public void verifyFeature(final ProductLicenseKey key, final Feature feature) throws LicensingException {
    // TODO
  }

  public boolean isValid() {
    return valid;
  }

  public ProductLicenseKey getKey() {
    return key;
  }

  public void setKey(final ProductLicenseKey key) {
    this.key = key;
  }

  public void setEnforcementPoints(CLMEnforcementPoint... enforcementPoints) {
    if (valid) {
      this.enforcementPoints.clear();

      for (CLMEnforcementPoint enforcementPoint : enforcementPoints) {
        this.enforcementPoints.add(enforcementPoint);
      }

      createKey();
    }
  }

  public void setApplicationLimit(int applicationLimit) {
    if (valid) {
      this.appCount = applicationLimit;
      createKey();
    }
  }

  public void setExpirationDate(Date date) {
    if (valid) {
      this.expirationDate = date;
      createKey();
    }
  }

  public void forceInstallFailure(boolean forceInstallFailure) {
    this.forceInstallFailure = forceInstallFailure;
  }
}
