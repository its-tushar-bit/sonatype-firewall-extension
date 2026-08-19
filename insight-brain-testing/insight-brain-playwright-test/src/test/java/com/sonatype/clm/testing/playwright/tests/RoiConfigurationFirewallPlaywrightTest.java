/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.pages.EditRoiConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.EditRoiConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.RoiConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.RoiConfigurationPageAssertions;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class RoiConfigurationFirewallPlaywrightTest
    extends AbstractRoiConfigurationPlaywrightTest
{
  private RoiConfigurationPage roiPage;

  private RoiConfigurationPageAssertions roiAssertions;

  private EditRoiConfigurationPage editRoiPage;

  private EditRoiConfigurationPageAssertions editRoiAssertions;

  @BeforeEach
  public void setUp() {
    // Backend /rest/roiConfiguration requires Lifecycle entitlement; frontend gates the Firewall
    // Metrics section on `selectHasFirewallLicense`. Both licenses needed for endpoint + section.
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_FIREWALL,
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);

    seedRoiConfigurationDefaultsIfMissing();
    seedRoiConfigurationIfMissing();

    playwrightRefreshOrOpen(RoiConfigurationPage.url());
    playwrightLogin();
    roiPage = new RoiConfigurationPage();
    roiAssertions = new RoiConfigurationPageAssertions(roiPage);
    editRoiPage = new EditRoiConfigurationPage();
    editRoiAssertions = new EditRoiConfigurationPageAssertions(editRoiPage);
  }

  @AfterEach
  public void cleanup() {
    playwrightLogout();
    deleteRoiConfiguration();
  }

  @Test
  @Tag("regression")
  public void testRoiConfiguration_firewallMetricsSectionRendersWithFirewallLicense() {
    roiAssertions.shouldShowFirewallMetricsSection();
  }

  @Test
  @Tag("regression")
  public void testEditRoiConfiguration_firewallInputsRenderWithFirewallLicense() {
    roiPage.editLink().click();
    editRoiAssertions.shouldShowFirewallInputs();
  }
}
