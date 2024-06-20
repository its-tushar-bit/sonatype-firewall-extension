/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.SbomManagerDashboardPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SbomManagerDashboardPageTest
    extends AbstractMtiqFunctionalTest
{
  private final SbomManagerDashboardPage sbomManagerDashboardPage = new SbomManagerDashboardPage();

  @Before
  public void before() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Test
  public void testDashboard_PageHeader() {
    refreshOrOpen(SbomManagerDashboardPage.url());

    sbomManagerDashboardPage.title()
        .shouldBe(visible)
        .shouldHave(text("Dashboard"));
  }

  @Test
  public void testDashboard_SbomManagerDisabled() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    refreshOrOpen(SbomManagerDashboardPage.url());

    sbomManagerDashboardPage.title().shouldNotBe(visible);
    sbomManagerDashboardPage.errorAlert().shouldBe(visible);
  }
}
