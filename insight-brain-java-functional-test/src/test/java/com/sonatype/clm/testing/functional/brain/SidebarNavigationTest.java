/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.functional.pages.ApiPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.clm.testing.functional.pages.VulnerabilitySearchPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.product.license.ProductLicenseService;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.CSS_SIDEBAR_CLOSED;
import static com.sonatype.clm.testing.functional.elements.CLM.CSS_SIDEBAR_OPEN;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;

public class SidebarNavigationTest
        extends AbstractFunctionalTest
{
  @Before
  public void before() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    // logout if not already logged out
    hardreset();
  }

  @Test
  public void testSidebar_DefaultsToOpen() {
    SidebarNavigation.container()
            .shouldBe(visible)
            .shouldHave(CSS_SIDEBAR_OPEN)
            .shouldNotHave(CSS_SIDEBAR_CLOSED);
  }

  @Test
  public void testSidebar_Toggles() {
    SidebarNavigation.container().shouldBe(visible).shouldHave(CSS_SIDEBAR_OPEN);
    SidebarNavigation.toggleNavigationButton().shouldBe(visible).click();
    SidebarNavigation.container().shouldBe(visible).shouldHave(CSS_SIDEBAR_CLOSED);
    SidebarNavigation.toggleNavigationButton().shouldBe(visible).click();
    SidebarNavigation.container().shouldBe(visible).shouldHave(CSS_SIDEBAR_OPEN);
  }

  @Test
  public void testProductVersion() throws Exception {
    // version is the same as what we display in the startup message for product and version except that:
    // 1. point release numbers are not included unless nonzero
    // 2. build numbers (or SNAPSHOT) are not included in the version number
    String version = testCLMServer.getCLMServer().getInstance(VersionService.class).getLogDisplayVersion();
    version = version.substring(0, version.indexOf("-")).replace(".0", "");
    String productVersion =
            testCLMServer.getCLMServer().getInstance(ProductLicenseService.class).validateLicense().productEdition
                    + " release " + version;
    SidebarNavigation.productVersion().shouldHave(text(productVersion));
  }

  @Test
  public void testDashboardIcon_DashboardAvailable() {
    SidebarNavigation.dashboardNavigationButton().shouldBe(visible);
  }

  @Test
  public void testDashboardIcon_DashboardNotAvailable() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();
    SidebarNavigation.policiesNavigationButton().shouldBe(visible);
    SidebarNavigation.dashboardNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testReportingIcon_ReportsListAvailable() {
    SidebarNavigation.reportingNavigationButton().shouldBe(visible);
  }

  @Test
  public void testReportingIcon_ReportsListNotAvailable() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.REPORTS_LIST_DISABLED, "true");
    refresh();
    SidebarNavigation.policiesNavigationButton().shouldBe(visible);
    SidebarNavigation.reportingNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testNavigation_ToDashboard() {
    SidebarNavigation.dashboardNavigationButton().click();
    waitUntilUrl(DashboardPage.url());
  }

  @Test
  public void testNavigation_ToReporting() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    SidebarNavigation.reportingNavigationButton().click();
    waitUntilUrl(ReportListPage.url());
  }

  @Test
  public void testNavigation_ToPolicies() {
    SidebarNavigation.policiesNavigationButton().click();
    waitUntilUrl(OwnerSummaryPage.urlToRootOrg());
  }

  @Test
  public void testNavigation_ToLabs() {
    SidebarNavigation.labsNavigationButton().click();
    waitUntilUrl(SuccessMetricsReportListPage.url());
  }

  @Test
  public void testNavigation_ToVulnerabilityDetails() {
    SidebarNavigation.vulnerabilityDetailsNavigationButton().click();
    waitUntilUrl(VulnerabilitySearchPage.url());
  }

  @Test
  public void testAdvancedSearchNavigationButton_HiddenByDefault() {
    SidebarNavigation.advancedSearchNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testNavigation_ToAdvancedSearch() {
    new SystemConfigurationPropertyDAO().update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "true"));
    refresh();
    SidebarNavigation.advancedSearchNavigationButton().shouldBe(visible).click();
    waitUntilUrl(AdvancedSearchPage.url());
  }

  @Test
  public void testNavigation_ToAdvancedSearch_NonAdminUser() {
    try {
      new SystemConfigurationPropertyDAO().update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "true"));
      User user = tempEntity.newUser();
      refreshOrOpen(DashboardPage.url());
      logout();
      login(user.getUsername(), user.getPassword());
      SidebarNavigation.advancedSearchNavigationButton().shouldBe(visible).click();
      waitUntilUrl(AdvancedSearchPage.url());
    }
    finally {
      logout();
      refreshOrOpen(DashboardPage.url());
      loginAsAdmin();
    }
  }

  @Test
  public void testFirewallNavigationButton_ShowByDefault() {
    SidebarNavigation.firewallNavigationButton().shouldBe(visible);
  }

  @Test
  public void testFirewallNavigationButton_FeaturesUnavailableHidden() {
    setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);
    refresh();
    SidebarNavigation.firewallNavigationButton().shouldBe(hidden);
    testProductLicense.reset();
    setMissingFeature(LicensedFeature.RELEASE_INTEGRITY);
    refresh();
    SidebarNavigation.firewallNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testNavigation_ToFirewall() {
    SidebarNavigation.firewallNavigationButton().shouldBe(visible).click();
    waitUntilUrl(FirewallPage.url());
  }

  @Test
  public void testLegalNavigationButton_HiddenByDefault() {
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);
    refresh();
    SidebarNavigation.legalNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testLegalNavigationButton_FeatureAvailableAdmin() {
    SidebarNavigation.legalNavigationButton().shouldBe(visible);
  }

  @Test
  public void testLegalNavigationButton_FeatureAvailableNonAdmin() {
    User user = tempEntity.newUser();
    refreshOrOpen(DashboardPage.url());
    logout();
    login(user.getUsername(), user.getPassword());
    SidebarNavigation.legalNavigationButton().shouldBe(visible);
  }

  @Test
  public void testLegalNavigation_toLegalDashboard() throws Exception {
    SidebarNavigation.legalNavigationButton().shouldBe(visible).click();
    waitUntilUrl(BaseUrl.resolvePageUrl("/legal/dashboard"));
  }

  @Test
  public void testApiNavigationButton_HiddenByDefault() {
    SidebarNavigation.apiNavigationButton().shouldNot(exist);
  }

  @Test
  public void testApiNavigationButton_FeatureAvailableAdmin() {
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(true);
    refresh();
    SidebarNavigation.apiNavigationButton().shouldBe(visible);
  }

  @Test
  public void testApiNavigationButton_FeatureAvailableNonAdmin() {
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(true);
    User user = tempEntity.newUser();
    refreshOrOpen(DashboardPage.url());
    logout();
    login(user.getUsername(), user.getPassword());
    SidebarNavigation.apiNavigationButton().shouldBe(visible);
  }

  @Test
  public void testNavigation_ToApi() {
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(true);
    refresh();
    SidebarNavigation.apiNavigationButton().click();
    waitUntilUrl(ApiPage.url());
  }

  @Test
  public void testDataInsightsNavigation_ShowWhenEnabled() {
    setFeatures(LicensedFeature.DATA_INSIGHTS);
    refresh();
    SidebarNavigation.dataInsightsNavigationButton().shouldBe(visible);
  }

  @Test
  public void testDataInsightsNavigation_HiddenWhenDisabled() {
    SidebarNavigation.dataInsightsNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testDataInsightsNavigation_toDataInsights() throws Exception {
    setFeatures(LicensedFeature.DATA_INSIGHTS);
    refresh();
    SidebarNavigation.dataInsightsNavigationButton().shouldBe(visible).click();
    waitUntilUrl(BaseUrl.resolvePageUrl("/dataInsights"));
  }
}
