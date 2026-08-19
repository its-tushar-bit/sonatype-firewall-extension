/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license.entitlement;

import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EntitlementRequiredExceptionTest
{
  @Test
  public void testCarriesFeature() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.CUSTOM_POLICIES);
    assertThat(exception.getFeature()).isEqualTo(LicensedFeature.CUSTOM_POLICIES);
  }

  @Test
  public void testCarriesUpsellInfo() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.BULK_WAIVERS);
    assertThat(exception.getUpsellInfo()).isNotNull();
    assertThat(exception.getUpsellInfo().getMessage()).contains("Bulk waivers");
  }

  @Test
  public void testMessageMatchesUpsellInfo() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.CUSTOM_POLICIES);
    assertThat(exception.getMessage()).isEqualTo(exception.getUpsellInfo().getMessage());
  }

  @Test
  public void testUnknownFeatureUsesDefault() {
    EntitlementRequiredException exception = new EntitlementRequiredException(LicensedFeature.DASHBOARD);
    assertThat(exception.getFeature()).isEqualTo(LicensedFeature.DASHBOARD);
    assertThat(exception.getUpsellInfo()).isNotNull();
    assertThat(exception.getMessage()).contains("not available in your current plan");
  }
}
