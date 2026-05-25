/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.sonatype.licensing.product.ProductLicenseKey;

@Named
@Singleton
public class TestProductLicense
    extends DefaultProductLicense
{
  private final TestProductLicenseManager testProductLicenseManager;

  private Optional<Integer> maxApplications;

  private Optional<Integer> maxSboms;

  @Inject
  public TestProductLicense(
      TestProductLicenseManager testProductLicenseManager,
      DeveloperEnablementService developerEnablementService)
  {
    this(testProductLicenseManager, true, developerEnablementService);
  }

  public TestProductLicense(
      TestProductLicenseManager testProductLicenseManager,
      boolean resetOnConstruct,
      DeveloperEnablementService developerEnablementService)
  {
    super(developerEnablementService);
    this.testProductLicenseManager = testProductLicenseManager;
    if (resetOnConstruct) {
      reset();
    }
  }

  /**
   * Perform initial set up for a test license. Needs to be called before first use.
   */
  public void reset() {
    try {
      testProductLicenseManager.reset();
      maxApplications = null;
      maxSboms = null;
      ProductLicenseKey productLicenseKey =
          testProductLicenseManager.getLicenseDetails(new ByteArrayInputStream(new byte[1]));
      // Default test license excludes HOSTED stage from the licensed set: hosted-repository
      // synchronous enforcement (CLM-39870) is gated by the HOSTED_REPOSITORY_EVALUATION
      // feature flag, and behaves the same way real licenses do today (HOSTED is not auto-
      // included in any product tier yet). Tests that explicitly need HOSTED licensed should
      // call setStageTypes(...) themselves.
      Set<StageType> defaultStageTypes = StageTypes.getAll()
          .stream()
          .filter(stageType -> !StageTypes.HOSTED.equals(stageType))
          .collect(Collectors.toCollection(HashSet::new));
      set(productLicenseKey, "1234",
          new HashSet<>(
              Arrays.asList(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, ProductLicenseDetails.PRODUCT_FIREWALL,
                  ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY,
                  ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD)),
          EnumSet.allOf(LicensedFeature.class), defaultStageTypes,
          Collections.singleton(ProductLicensingModel.LEGACY),
          100, 50, 45, 50);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Integer getMaxApplications() {
    if (maxApplications != null) {
      return maxApplications.orElse(null);
    }
    return super.getMaxApplications();
  }

  @Override
  public Integer getMaxSboms() {
    if (maxSboms != null) {
      return maxSboms.orElse(null);
    }
    return super.getMaxSboms();
  }

  public void setMaxApplications(Integer maxApplications) {
    this.maxApplications = Optional.ofNullable(maxApplications);
  }

  public void setMaxSbom(Integer maxSboms) {
    this.maxSboms = Optional.ofNullable(maxSboms);
  }

  @Override
  public Set<LicensedFeature> getFeatures() {
    // features are normally derived based on the products
    // for precise testing, we allow them to be manually overridden to a specific set
    Set<LicensedFeature> features = testProductLicenseManager.getFeatures();
    if (features != null) {
      return features;
    }
    return super.getFeatures();
  }

  public void setFeatures(LicensedFeature... features) {
    testProductLicenseManager.setFeatures(features);
  }

  public void setMissingFeatures(LicensedFeature feature, LicensedFeature... features) {
    testProductLicenseManager
        .setFeatures(EnumSet.complementOf(EnumSet.of(feature, features)).toArray(new LicensedFeature[0]));
  }

  @Override
  public Set<String> getProducts() {
    Set<String> products = testProductLicenseManager.getProducts();
    if (products != null) {
      return products;
    }
    return super.getProducts();
  }

  public void setProducts(String... products) {
    testProductLicenseManager.setProducts(products);
  }

  @Override
  public Set<StageType> getStageTypes() {
    // stage types are normally derived based on the products
    // for precise testing, we allow them to be manually overridden to a specific set
    Set<StageType> stageTypes = testProductLicenseManager.getStageTypes();
    if (stageTypes != null) {
      return stageTypes;
    }
    return super.getStageTypes();
  }

  public void setStageTypes(StageType... stageTypes) {
    testProductLicenseManager.setStageTypes(stageTypes);
  }

  public void setStageTypes(Collection<StageType> stageTypes) {
    setStageTypes(stageTypes.toArray(new StageType[stageTypes.size()]));
  }
}
