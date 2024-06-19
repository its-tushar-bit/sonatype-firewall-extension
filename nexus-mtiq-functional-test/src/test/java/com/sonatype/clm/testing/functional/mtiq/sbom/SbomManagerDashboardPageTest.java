/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.SbomManagerDashboardPage;

import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SbomManagerDashboardPageTest
    extends AbstractMtiqFunctionalTest
{
  private final SbomManagerDashboardPage sbomManagerDashboardPage = new SbomManagerDashboardPage();

  @Test
  public void testDashboardPageHeader() {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen(sbomManagerDashboardPage.url());
    loginAsAdmin();
    sbomManagerDashboardPage.title().shouldHave(text("Dashboard")).shouldBe(visible);
  }

  @Test
  public void testFeatureDisabled_Error() {
    setMissingFeature(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen(sbomManagerDashboardPage.url());
    loginAsAdmin();
    sbomManagerDashboardPage.title().shouldNotBe(visible);
    sbomManagerDashboardPage.errorAlert().shouldBe(visible);
  }
}
