/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.product.license.CLMFeature;
import com.sonatype.insight.brain.product.license.GuideFeature;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.internal.DefaultLicenseKey;

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
  public ProductLicenseKey getLicenseDetails() {
    return mockProductLicenseManager.getLicenseDetails();
  }

  @Override
  public ProductLicenseKey getLicenseDetails(InputStream licenseFile) throws IOException {
    return mockProductLicenseManager.getLicenseDetails(licenseFile);
  }

  @Override
  public void installLicense(InputStream licenseFile) throws IOException {
    mockProductLicenseManager.installLicense(licenseFile);
  }

  @Override
  public void uninstallLicense() {
    wasChanged = true;
    mockProductLicenseManager.uninstallLicense();
  }

  @Override
  public void verifyFeature(ProductLicenseKey key, org.sonatype.licensing.feature.Feature feature) {
    mockProductLicenseManager.verifyFeature(key, feature);
  }

  @Override
  public void verifyLicenseAndFeature(org.sonatype.licensing.feature.Feature feature) {
    mockProductLicenseManager.verifyLicenseAndFeature(feature);
  }

  public boolean wasChanged() {
    return wasChanged;
  }

  public void reset() {
    wasChanged = false;
    mockProductLicenseManager = new MockProductLicenseManager();
  }

  public boolean isValid() {
    return mockProductLicenseManager.isValid();
  }

  public void setApplicationLimit(Integer applicationLimit) {
    wasChanged = true;
    mockProductLicenseManager.setApplicationLimit(applicationLimit);
  }

  public Integer getApplicationLimit() {
    return mockProductLicenseManager.applicationLimit;
  }

  public void setMaxUsers(Integer maxUsers) {
    wasChanged = true;
    mockProductLicenseManager.setProperty(ProductLicenseDetails.PROPERTY_MAX_USERS, maxUsers);
  }

  public void setMaxFirewallUsers(Integer maxFirewallUsers) {
    wasChanged = true;
    mockProductLicenseManager.setProperty(ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS, maxFirewallUsers);
  }

  public void setMaxSboms(Integer maxSboms) {
    wasChanged = true;
    mockProductLicenseManager.setProperty(ProductLicenseDetails.PROPERTY_MAX_SBOMS, maxSboms);
  }

  public void setFeatures(LicensedFeature... features) {
    wasChanged = true;
    mockProductLicenseManager.setFeatures(features);
  }

  public Set<LicensedFeature> getFeatures() {
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

  public void setAllowedFeatureIds(String... featureIds) {
    wasChanged = true;
    mockProductLicenseManager.allowedFeatureIds = new HashSet<>(Arrays.asList(featureIds));
  }

  public void setForceUninstallFailure(boolean forceUninstallFailure) {
    wasChanged = true;
    mockProductLicenseManager.setForceUninstallFailure(forceUninstallFailure);
  }

  public Set<String> getProducts() {
    if (mockProductLicenseManager.products == null) {
      return null;
    }
    return new HashSet<>(Arrays.asList(mockProductLicenseManager.products));
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
    mockProductLicenseManager.setProperty(ProductLicenseDetails.PROPERTY_VERSION, Integer.toString(version));
  }

  private static class MockProductLicenseManager
      implements ProductLicenseManager
  {
    private volatile boolean valid = true;

    private volatile ProductLicenseKey key;

    private Integer applicationLimit = 100;

    private Date expirationDate = new Date(System.currentTimeMillis() + 6000 * 1000);

    private String[] products = {
      ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
      ProductLicenseDetails.PRODUCT_FIREWALL_V2,
      ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY_V2,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_CLOUD,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_FIREWALL_SAAS,
      ProductLicenseDetails.PRODUCT_LIFECYCLE_FOUNDATION_SAAS,
      ProductLicenseDetails.PRODUCT_AUDITOR_SAAS,
    };

    private Set<LicensedFeature> features;

    private Set<StageType> stageTypes;

    private final Map<String, String> properties = new HashMap<>();

    private boolean forceInstallIOFailure = false;

    private boolean forceUninstallFailure = false;

    private boolean forceVerificationFailure;

    private Set<String> allowedFeatureIds;

    public MockProductLicenseManager() {
      properties.put(ProductLicenseDetails.PROPERTY_VERSION, "1");
      properties.put(ProductLicenseDetails.PROPERTY_MAX_FIREWALL_USERS, "45");
      setKey();
    }

    /**
     * To verify proper use of the input stream (e.g. only read once), mimic the real component by at least consuming
     * the stream, deeming it invalid if empty.
     */
    private void readLicenseFile(InputStream licenseFile) throws IOException {
      if (licenseFile.read() < 0) {
        throw new LicensingException("Invalid license file");
      }
      while (licenseFile.read() >= 0) {
        // consume the stream
      }
    }

    @Override
    public void installLicense(final InputStream licenseFile) throws IOException {
      readLicenseFile(licenseFile);

      if (forceInstallIOFailure) {
        throw new IOException("An IO error occurred");
      }

      valid = true;
      setKey();
    }

    private ProductLicenseKey createKey() {
      Map<String, org.sonatype.licensing.feature.Feature> featureMap = new HashMap<>();
      featureMap.put(CLMFeature.ID, new CLMFeature());
      featureMap.put(GuideFeature.ID, new GuideFeature());
      Properties properties = new Properties();
      if (products != null) {
        properties.put(ProductLicenseDetails.PROPERTY_PRODUCTS, String.join(",", products));
      }
      properties.put(ProductLicenseDetails.PROPERTY_MAX_USERS, Integer.toString(50));

      if (applicationLimit != null) {
        properties.put(ProductLicenseDetails.PROPERTY_APPLICATION_LIMIT, applicationLimit.toString());
      }

      properties.put(ProductLicenseDetails.PROPERTY_MAX_SBOMS, Integer.toString(50));

      properties.putAll(this.properties);

      DefaultLicenseKey key = new DefaultLicenseKey(featureMap);

      // effective date is yesterday
      key.setEffectiveDate(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)));
      key.setExpirationDate(expirationDate);
      key.setContactName("Billy");
      key.setContactCompany("Acme");
      key.setContactEmailAddress("billy@example.com");
      key.setProperties(properties);

      return key;
    }

    private void setKey() {
      this.key = createKey();
    }

    @Override
    public void uninstallLicense() {
      if (forceUninstallFailure) {
        throw new RuntimeException("Uninstall failed");
      }
      valid = false;
      key = null;
      products = null;
    }

    @Override
    public ProductLicenseKey getLicenseDetails() {
      if (!valid) {
        throw new LicensingException("Not licensed");
      }
      return key;
    }

    @Override
    public ProductLicenseKey getLicenseDetails(final InputStream licenseFile) throws IOException {
      readLicenseFile(licenseFile);
      return createKey();
    }

    @Override
    public void verifyLicenseAndFeature(final org.sonatype.licensing.feature.Feature feature) {
    }

    @Override
    public void verifyFeature(final ProductLicenseKey key, final org.sonatype.licensing.feature.Feature feature) {
      if (forceVerificationFailure) {
        throw new LicensingException("License does not permit use of feature '" + feature.getId() + "'");
      }
      if (allowedFeatureIds != null && !allowedFeatureIds.contains(feature.getId())) {
        throw new LicensingException("License does not permit use of feature '" + feature.getId() + "'");
      }
    }

    public boolean isValid() {
      return valid;
    }

    public void setFeatures(LicensedFeature... features) {
      if (valid) {
        this.features = EnumSet.noneOf(LicensedFeature.class);

        this.features.addAll(Arrays.asList(features));

        setKey();
      }
    }

    public void setStageTypes(StageType... stageTypes) {
      if (valid) {
        this.stageTypes = new LinkedHashSet<>();
        Collections.addAll(this.stageTypes, stageTypes);
        setKey();
      }
    }

    public void setApplicationLimit(Integer applicationLimit) {
      if (valid) {
        this.applicationLimit = applicationLimit;
        setKey();
      }
    }

    public void setProducts(String... products) {
      if (valid) {
        this.products = products;
        setKey();
      }
    }

    public void setExpirationDate(Date date) {
      if (valid) {
        this.expirationDate = date;
        setKey();
      }
    }

    public void setForceInstallIOFailure(boolean forceInstallIOFailure) {
      this.forceInstallIOFailure = forceInstallIOFailure;
    }

    public void setForceVerificationFailure(boolean forceVerificationFailure) {
      this.forceVerificationFailure = forceVerificationFailure;
    }

    public void setForceUninstallFailure(boolean forceUninstallFailure) {
      this.forceUninstallFailure = forceUninstallFailure;
    }

    public void setProperty(String key, Object value) {
      if (valid) {
        if (value == null) {
          properties.remove(key);
        }
        else {
          properties.put(key, value.toString());
        }
        setKey();
      }
    }
  }
}
