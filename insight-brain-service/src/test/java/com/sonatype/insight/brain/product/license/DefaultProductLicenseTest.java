/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.Set;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers AI Developer being granted by an organization's opt-in rather than by a license entitlement.
 */
class DefaultProductLicenseTest
{
  private static final Set<String> LIFECYCLE = Set.of(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);

  private static final String OPT_IN = "admin,2026-08-17T09:15:00Z";

  @Test
  void lifecycleWithoutOptInHasNoAiDeveloper() {
    DefaultProductLicense license = license(LIFECYCLE, Set.of(), null);

    assertThat(license.hasFeature(LicensedFeature.AI_DEVELOPER)).isFalse();
    assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER)).isFalse();
    assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)).isFalse();
  }

  @Test
  void blankOptInHasNoAiDeveloper() {
    assertThat(license(LIFECYCLE, Set.of(), "   ").hasFeature(LicensedFeature.AI_DEVELOPER)).isFalse();
  }

  @Test
  void optInGrantsAiDeveloperToLifecycleWithoutEntitlement() {
    DefaultProductLicense license = license(LIFECYCLE, Set.of(), OPT_IN);

    assertThat(license.hasFeature(LicensedFeature.AI_DEVELOPER)).isTrue();
    assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER)).isTrue();
    assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)).isTrue();
  }

  @Test
  void optInGrantsNothingElseAndLeavesTheLicenseContentsAlone() {
    DefaultProductLicense license = license(LIFECYCLE, Set.of(), OPT_IN);

    assertThat(license.hasFeature(LicensedFeature.GUIDE)).isFalse();
    assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)).isFalse();
    assertThat(license.getProducts()).isEqualTo(LIFECYCLE);
    assertThat(license.getFeatures()).isEmpty();
  }

  @Test
  void optInGrantsNothingWithoutALifecycleProduct() {
    DefaultProductLicense license = license(Set.of(ProductLicenseDetails.PRODUCT_FIREWALL), Set.of(), OPT_IN);

    assertThat(license.hasFeature(LicensedFeature.AI_DEVELOPER)).isFalse();
    assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER)).isFalse();
    assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)).isFalse();
  }

  @Test
  void entitledLicenseGrantsAiDeveloperWithoutOptIn() {
    DefaultProductLicense license = license(
        Set.of(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_AI_DEVELOPER),
        Set.of(LicensedFeature.AI_DEVELOPER), null);

    assertThat(license.hasFeature(LicensedFeature.AI_DEVELOPER)).isTrue();
    assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER)).isTrue();
  }

  @Test
  void unreadableConfigurationHasNoAiDeveloper() {
    SystemConfigurationPropertyDAO dao = mock(SystemConfigurationPropertyDAO.class);
    when(dao.getByName(anyString())).thenThrow(new IllegalStateException("database is down"));

    assertThat(licenseReading(LIFECYCLE, Set.of(), dao).hasFeature(LicensedFeature.AI_DEVELOPER)).isFalse();
  }

  private static DefaultProductLicense license(
      Set<String> products,
      Set<LicensedFeature> features,
      String optInValue)
  {
    SystemConfigurationPropertyDAO dao = mock(SystemConfigurationPropertyDAO.class);
    when(dao.getByName(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN)).thenReturn(optInValue == null
        ? null
        : new SystemConfigurationProperty(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN, optInValue));
    return licenseReading(products, features, dao);
  }

  private static DefaultProductLicense licenseReading(
      Set<String> products,
      Set<LicensedFeature> features,
      SystemConfigurationPropertyDAO dao)
  {
    return new DefaultProductLicense(mock(DeveloperEnablementService.class), dao)
    {
      @Override
      public Set<String> getProducts() {
        return products;
      }

      @Override
      public Set<LicensedFeature> getFeatures() {
        return features;
      }
    };
  }
}
