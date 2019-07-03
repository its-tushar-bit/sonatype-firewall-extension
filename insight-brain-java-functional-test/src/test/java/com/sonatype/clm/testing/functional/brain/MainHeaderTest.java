/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.clm.testing.functional.pages.VulnerabilitySearchPage;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class MainHeaderTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    refreshOrOpen(ReportListPage.URL);
  }

  @Test
  public void testLoggedInUserName() {
    MainHeader.userMenu().userName().shouldBe(visible).shouldHave(text("Admin BuiltIn"));
  }

  @Test
  public void testProductVersion() throws Exception {
    // version is the same as what we display in the startup message for product and version except that:
    // 1. point release numbers are not included unless nonzero
    // 2. build numbers (or SNAPSHOT) are not included in the version number
    String version = new VersionService().getLogDisplayVersion();
    version = version.substring(0, version.indexOf("-")).replace(".0", "");
    String productVersion = clmLicenseManager.getLicenseSummary().productEdition + " release " + version;
    MainHeader.productVersion().shouldHave(text(productVersion));
  }

  @Test
  public void testDashboardIcon_DashboardLicensed() {
    MainHeader.dashboardNavigationButton().shouldBe(visible);
  }

  @Test
  public void testDashboardIcon_DashboardNotLicensed() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();
    MainHeader.policiesNavigationButton().shouldBe(visible);
    MainHeader.dashboardNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testDefaultPage_DashboardLicensed() {
    refreshOrOpen(IndexPage.url());
    waitUntilUrl(DashboardPage.URL);
  }

  @Test
  public void testDefaultPage_DashboardNotLicensed() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refreshOrOpen(IndexPage.url());
    waitUntilUrl(ReportListPage.URL);
  }

  @Test
  public void testNavigation_ToDashboard() {
    MainHeader.dashboardNavigationButton().click();
    waitUntilUrl(DashboardPage.URL);
  }

  @Test
  public void testNavigation_ToReporting() {
    refreshOrOpen(OrganizationManagementPage.ROOT_ORG_URL);
    MainHeader.reportingNavigationButton().click();
    waitUntilUrl(ReportListPage.URL);
  }

  @Test
  public void testNavigation_ToPolicies() {
    MainHeader.policiesNavigationButton().click();
    waitUntilUrl(OrganizationManagementPage.ROOT_ORG_URL);
  }

  @Test
  public void testNavigation_ToLabs() {
    MainHeader.labsNavigationButton().click();
    waitUntilUrl(SuccessMetricsReportListPage.URL);
  }

  @Test
  public void testNavigation_ToVulnerabilityDetails() {
    MainHeader.vulnerabilityDetailsNavigationButton().click();
    waitUntilUrl(VulnerabilitySearchPage.url());
  }
}
