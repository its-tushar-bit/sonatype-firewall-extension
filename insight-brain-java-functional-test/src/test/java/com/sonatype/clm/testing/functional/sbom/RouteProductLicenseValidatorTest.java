/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerDashboardPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;

public class RouteProductLicenseValidatorTest
    extends AbstractFunctionalTest
{
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void beforeEachMethod() {
    systemConfigurationPropertyDAO = lookup(SystemConfigurationPropertyDAO.class);
    systemConfigurationPropertyDAO.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "true"));

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen(IndexPage.url());
  }

  @Test
  public void testRouteProductLicenseValidator_nonPermittedPathRedirectsToSbomManagerDashboard() {
    final SbomManagerDashboardPage sbomManagerDashboardPage = new SbomManagerDashboardPage();
    Application application = tempEntity.newApplicationWithParent("test-app");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    waitUntilUrl(SbomManagerDashboardPage.url());

    sbomManagerDashboardPage.title()
        .shouldBe(visible)
        .shouldHave(text("SBOM Manager Dashboard"));
  }

  @Test
  public void testRouteProductLicenseValidator_nonSbomOnlyPermittedPath_isAllowedWithNonSbomOnlyLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER, ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.SOURCE_CONTROL);
    Application application = tempEntity.newApplicationWithParent("test-app");

    refresh(); // Force page reload to ensure the app is fully reloaded
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    SourceControlEditorPage.title()
        .shouldBe(visible)
        .shouldHave(text("Source Control Configuration"));
  }

  @Test
  public void testRouteProductLicenseValidator_alwaysPermittedPaths() {
    refreshOrOpen(GettingStartedPage.url());

    final GettingStartedPage gettingStartedPage = new GettingStartedPage();
    gettingStartedPage.productLicenseSummary().shouldBe(visible);

    refreshOrOpen(ProductLicensePage.url());
    ProductLicensePage.licensedSboms().shouldBe(visible);
    ProductLicensePage.products().shouldHave(texts("Sonatype SBOM Manager"));
  }

  @Test
  public void testRouteProductLicenseValidator_redirectsToSbomDashboard() {
    refreshOrOpen(IndexPage.url());
    waitUntilUrl(SbomManagerDashboardPage.url());
    final SbomManagerDashboardPage sbomManagerDashboardPage = new SbomManagerDashboardPage();
    sbomManagerDashboardPage.title().shouldBe(visible).shouldHave(text("SBOM Manager Dashboard"));
  }

}
