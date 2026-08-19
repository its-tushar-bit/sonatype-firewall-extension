/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license.entitlement;

import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EntitlementUpsellRegistryTest
{
  @Test
  public void testGetUpsellInfo_CustomPolicies() {
    UpsellInfo info = EntitlementUpsellRegistry.getUpsellInfo(LicensedFeature.CUSTOM_POLICIES);
    assertThat(info).isNotNull();
    assertThat(info.getMessage()).contains("Custom policies");
    assertThat(info.getUpgradeHint()).contains("Enterprise");
    assertThat(info.getDocsUrl()).isNotNull();
    assertThat(info.getCtaUrl()).contains("sonatype.com");
  }

  @Test
  public void testGetUpsellInfo_BulkWaivers() {
    UpsellInfo info = EntitlementUpsellRegistry.getUpsellInfo(LicensedFeature.BULK_WAIVERS);
    assertThat(info).isNotNull();
    assertThat(info.getMessage()).contains("Bulk waivers");
    assertThat(info.getUpgradeHint()).contains("Enterprise");
  }

  @Test
  public void testGetUpsellInfo_AutoWaiverManagement() {
    UpsellInfo info = EntitlementUpsellRegistry.getUpsellInfo(LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    assertThat(info).isNotNull();
    assertThat(info.getMessage()).contains("Auto waivers");
  }

  @Test
  public void testGetUpsellInfo_WaiverRequestWorkflow() {
    UpsellInfo info = EntitlementUpsellRegistry.getUpsellInfo(LicensedFeature.WAIVER_REQUEST_WORKFLOW);
    assertThat(info).isNotNull();
    assertThat(info.getMessage()).contains("Waiver request workflow");
  }

  @Test
  public void testGetUpsellInfo_CustomApplicationCategories() {
    UpsellInfo info = EntitlementUpsellRegistry.getUpsellInfo(LicensedFeature.CUSTOM_APPLICATION_CATEGORIES);
    assertThat(info).isNotNull();
    assertThat(info.getMessage()).contains("Custom application categories");
  }

  @Test
  public void testGetUpsellInfo_CustomComponentLabels() {
    UpsellInfo info = EntitlementUpsellRegistry.getUpsellInfo(LicensedFeature.CUSTOM_COMPONENT_LABELS);
    assertThat(info).isNotNull();
    assertThat(info.getMessage()).contains("Custom component labels");
  }

  @Test
  public void testGetUpsellInfo_CustomLicenseThreatGroups() {
    UpsellInfo info = EntitlementUpsellRegistry.getUpsellInfo(LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS);
    assertThat(info).isNotNull();
    assertThat(info.getMessage()).contains("Custom license threat groups");
  }

  @Test
  public void testGetUpsellInfo_UnknownFeature_ReturnsDefault() {
    UpsellInfo info = EntitlementUpsellRegistry.getUpsellInfo(LicensedFeature.DASHBOARD);
    assertThat(info).isNotNull();
    assertThat(info.getMessage()).contains("not available in your current plan");
    assertThat(info.getCtaUrl()).contains("sonatype.com");
  }

  @Test
  public void testAllRegisteredFeaturesHaveDocsUrl() {
    LicensedFeature[] registeredFeatures = {
      LicensedFeature.CUSTOM_POLICIES,
      LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
      LicensedFeature.CUSTOM_COMPONENT_LABELS,
      LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
      LicensedFeature.AUTO_WAIVER_MANAGEMENT,
      LicensedFeature.WAIVER_REQUEST_WORKFLOW,
      LicensedFeature.BULK_WAIVERS
    };
    for (LicensedFeature feature : registeredFeatures) {
      UpsellInfo info = EntitlementUpsellRegistry.getUpsellInfo(feature);
      assertThat(info.getDocsUrl()).as("docs URL for " + feature.name()).isNotNull();
    }
  }
}
