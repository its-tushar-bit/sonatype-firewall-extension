/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.CLMFeature;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.Features;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.internal.DefaultLicenseKey;

import org.apache.commons.lang.StringUtils;

/**
 * Test/mock implementation for ProductLicenseManager.
 * <p>
 * It wraps a MockProductLicenseManager instance, which is simply replaced when the reset() method is called.
 */
@Named
@Singleton
public class TestProductLicenseManager
    implements ProductLicenseManager
{
  private MockProductLicenseManager mockProductLicenseManager = new MockProductLicenseManager();

  private boolean wasChanged;

  @Override
  public ProductLicenseKey getLicenseDetails() throws LicensingException {
    return mockProductLicenseManager.getLicenseDetails();
  }

  @Override
  public ProductLicenseKey getLicenseDetails(InputStream licenseFile) throws IOException, LicensingException {
    return mockProductLicenseManager.getLicenseDetails(licenseFile);
  }

  @Override
  public void installLicense(InputStream licenseFile) throws IOException, LicensingException {
    mockProductLicenseManager.installLicense(licenseFile);
  }

  @Override
  public void uninstallLicense() throws LicensingException {
    wasChanged = true;
    mockProductLicenseManager.uninstallLicense();
  }

  @Override
  public void verifyFeature(ProductLicenseKey key, Feature feature) throws LicensingException {
    mockProductLicenseManager.verifyFeature(key, feature);
  }

  @Override
  public void verifyLicenseAndFeature(Feature feature) throws LicensingException {
    mockProductLicenseManager.verifyLicenseAndFeature(feature);
  }

  public boolean wasChanged() {
    return wasChanged;
  }

  public void reset() {
    wasChanged = false;
    mockProductLicenseManager = new MockProductLicenseManager();
  }

  public void forceInstallLicenseFailure(boolean forceInstallFailure) {
    wasChanged = true;
    mockProductLicenseManager.forceInstallLicenseFailure(forceInstallFailure);
  }

  public boolean isValid() {
    return mockProductLicenseManager.isValid();
  }

  public void setApplicationLimit(int applicationLimit) {
    wasChanged = true;
    mockProductLicenseManager.setApplicationLimit(applicationLimit);
  }

  public void setEnforcementPoints(CLMEnforcementPoint... enforcementPoints) {
    wasChanged = true;
    mockProductLicenseManager.setEnforcementPoints(enforcementPoints);
  }

  public void setExpirationDate(Date date) {
    wasChanged = true;
    mockProductLicenseManager.setExpirationDate(date);
  }

  public void setForceInstallIOFailure(boolean forceInstallIOFailure) {
    wasChanged = true;
    mockProductLicenseManager.setForceInstallIOFailure(forceInstallIOFailure);
  }

  public void setProducts(String... products) {
    wasChanged = true;
    mockProductLicenseManager.setProducts(products);
  }

  public void setProperty(String key, String value) {
    wasChanged = true;
    mockProductLicenseManager.setProperty(key, value);
  }

  public void setVersion(int version) {
    wasChanged = true;
    mockProductLicenseManager.setVersion(version);
  }

  private static class MockProductLicenseManager
      implements ProductLicenseManager
  {
    private volatile boolean valid = true;

    private volatile ProductLicenseKey key;

    private int version = 1;

    private int appCount = 100;

    private Date expirationDate = new Date(System.currentTimeMillis() + 6000 * 1000);

    private String[] products = { ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION };

    private Set<CLMEnforcementPoint> enforcementPoints = new HashSet<>();

    private Map<String, String> properties = new HashMap<>();

    private boolean forceInstallLicenseFailure = false;

    private boolean forceInstallIOFailure = false;

    public MockProductLicenseManager() {
      resetEnforcementPoints();
    }

    public void resetEnforcementPoints() {
      enforcementPoints.clear();
      enforcementPoints.add(CLMEnforcementPoint.Build);
      enforcementPoints.add(CLMEnforcementPoint.Develop);
      enforcementPoints.add(CLMEnforcementPoint.Release);
      enforcementPoints.add(CLMEnforcementPoint.StageRelease);

      if (valid) {
        createKey();
      }
    }

    @Override
    public void installLicense(final InputStream licenseFile) throws IOException, LicensingException {
      if (forceInstallLicenseFailure) {
        throw new LicensingException("An error occurred");
      }

      if (forceInstallIOFailure) {
        throw new IOException("An IO error occurred");
      }

      valid = true;
      createKey();
    }

    private void createKey() {
      Map<String, Feature> featureMap = new HashMap<>();
      featureMap.put(CLMFeature.ID, new CLMFeature());
      Properties properties = new Properties();

      StringBuffer sb = new StringBuffer();

      for (CLMEnforcementPoint ep : enforcementPoints) {
        sb.append(ep.name()).append(",");
      }

      if (sb.length() > 0) {
        sb.setLength(sb.length() - 1);
      }

      properties.put(ProductLicenseDetails.PROPERTY_VERSION, Integer.toString(version));
      properties.put(ProductLicenseDetails.PROPERTY_PRODUCTS, StringUtils.join(products, ","));
      properties.put(ProductLicenseDetails.PROPERTY_ENFORCEMENT_POINTS, sb.toString());
      properties.put(ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT, Integer.toString(appCount));
      properties.putAll(this.properties);

      ProductLicenseKey key = new DefaultLicenseKey(new Features(featureMap));
      key.setEffectiveDate(new Date(System.currentTimeMillis() - 10000));
      key.setExpirationDate(expirationDate);
      key.setProperties(properties);
      this.key = key;
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

    public void setEnforcementPoints(CLMEnforcementPoint... enforcementPoints) {
      if (valid) {
        this.enforcementPoints.clear();

        for (CLMEnforcementPoint enforcementPoint : enforcementPoints) {
          this.enforcementPoints.add(enforcementPoint);
        }

        createKey();
      }
    }

    public void setVersion(int version) {
      if (valid) {
        this.version = version;
        createKey();
      }
    }

    public void setApplicationLimit(int applicationLimit) {
      if (valid) {
        this.appCount = applicationLimit;
        createKey();
      }
    }

    public void setProducts(String... products) {
      if (valid) {
        this.products = products;
        createKey();
      }
    }

    public void setExpirationDate(Date date) {
      if (valid) {
        this.expirationDate = date;
        createKey();
      }
    }

    public void forceInstallLicenseFailure(boolean forceInstallFailure) {
      this.forceInstallLicenseFailure = forceInstallFailure;
    }

    public void setForceInstallIOFailure(boolean forceInstallIOFailure) {
      this.forceInstallIOFailure = forceInstallIOFailure;
    }

    public void setProperty(String key, String value) {
      properties.put(key, value);
    }
  }
}
