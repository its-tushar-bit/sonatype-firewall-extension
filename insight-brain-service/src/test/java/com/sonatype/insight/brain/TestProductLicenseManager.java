/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.product.license.CLMFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.sonatype.licensing.LicensingException;
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
  public void verifyFeature(ProductLicenseKey key, org.sonatype.licensing.feature.Feature feature)
      throws LicensingException
  {
    mockProductLicenseManager.verifyFeature(key, feature);
  }

  @Override
  public void verifyLicenseAndFeature(org.sonatype.licensing.feature.Feature feature) throws LicensingException {
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

  public void setApplicationLimit(Integer applicationLimit) {
    wasChanged = true;
    mockProductLicenseManager.setApplicationLimit(applicationLimit);
  }

  public void setMaxFirewallUsers(Integer maxFirewallUsers) {
    wasChanged = true;
    mockProductLicenseManager.setMaxFirewallUsers(maxFirewallUsers);
  }

  public void setFeatures(Feature... features) {
    wasChanged = true;
    mockProductLicenseManager.setFeatures(features);
  }

  public Set<Feature> getFeatures() {
    return mockProductLicenseManager.features;
  }

  public void setStageTypes(StageType... stageTypes) {
    wasChanged = true;
    mockProductLicenseManager.setStageTypes(stageTypes);
  }

  public Set<StageType> getStageTypes() {
    return mockProductLicenseManager.stageTypes;
  }

  public Date getExpirationDate() {
    return mockProductLicenseManager.expirationDate;
  }

  public void setExpirationDate(Date date) {
    wasChanged = true;
    mockProductLicenseManager.setExpirationDate(date);
  }

  public void setForceInstallIOFailure(boolean forceInstallIOFailure) {
    wasChanged = true;
    mockProductLicenseManager.setForceInstallIOFailure(forceInstallIOFailure);
  }

  public void setForceVerificationFailure(boolean forceVerificationFailure) {
    wasChanged = true;
    mockProductLicenseManager.setForceVerificationFailure(forceVerificationFailure);
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

    private Integer applicationLimit = 100;

    private Integer maxFirewallUsers = 45;

    private Date expirationDate = new Date(System.currentTimeMillis() + 6000 * 1000);

    private String[] products = { ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
        ProductLicenseDetails.PRODUCT_FIREWALL };

    private Set<Feature> features;

    private Set<StageType> stageTypes;

    private Map<String, String> properties = new HashMap<>();

    private boolean forceInstallLicenseFailure = false;

    private boolean forceInstallIOFailure = false;

    private boolean forceVerificationFailure;

    public MockProductLicenseManager() {
      createKey();
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
      Map<String, org.sonatype.licensing.feature.Feature> featureMap = new HashMap<>();
      featureMap.put(CLMFeature.ID, new CLMFeature());
      Properties properties = new Properties();
      properties.put(ProductLicenseDetails.PROPERTY_VERSION, Integer.toString(version));
      properties.put(ProductLicenseDetails.PROPERTY_PRODUCTS, StringUtils.join(products, ","));
      properties.put(ProductLicenseDetails.PROPERTY_MAX_USERS, Integer.toString(50));

      if (applicationLimit != null) {
        properties.put(ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT, applicationLimit.toString());
      }

      if (maxFirewallUsers != null) {
        properties.put(ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS, Integer.toString(maxFirewallUsers));
      }

      properties.putAll(this.properties);

      DefaultLicenseKey key = new DefaultLicenseKey(new Features(featureMap));

      // effective date is yesterday
      key.setEffectiveDate(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)));
      key.setExpirationDate(expirationDate);
      key.setContactName("Billy");
      key.setContactCompany("Acme");
      key.setContactEmailAddress("billy@example.com");
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
    public void verifyLicenseAndFeature(final org.sonatype.licensing.feature.Feature feature)
        throws LicensingException
    {
    }

    @Override
    public void verifyFeature(final ProductLicenseKey key, final org.sonatype.licensing.feature.Feature feature)
        throws LicensingException
    {
      if (forceVerificationFailure) {
        throw new LicensingException("License does not permit use of feature '" + feature.getId() + "'");
      }
    }

    public boolean isValid() {
      return valid;
    }

    public void setFeatures(Feature... features) {
      if (valid) {
        this.features = EnumSet.noneOf(Feature.class);

        for (Feature feature : features) {
          this.features.add(feature);
        }

        createKey();
      }
    }

    public void setStageTypes(StageType... stageTypes) {
      if (valid) {
        this.stageTypes = new LinkedHashSet<>();
        Collections.addAll(this.stageTypes, stageTypes);
        createKey();
      }
    }

    public void setVersion(int version) {
      if (valid) {
        this.version = version;
        createKey();
      }
    }

    public void setApplicationLimit(Integer applicationLimit) {
      if (valid) {
        this.applicationLimit = applicationLimit;
        createKey();
      }
    }

    public void setMaxFirewallUsers(Integer maxFirewallUsers) {
      if (valid) {
        this.maxFirewallUsers = maxFirewallUsers;
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

    public void setForceVerificationFailure(boolean forceVerificationFailure) {
      this.forceVerificationFailure = forceVerificationFailure;
    }

    public void setProperty(String key, String value) {
      properties.put(key, value);
    }
  }
}
