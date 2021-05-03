/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.AgeFilter;
import com.sonatype.clm.testing.functional.elements.HelpMenu;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.elements.UserMenu;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.clm.testing.functional.pages.VulnerabilitySearchPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.product.license.ProductLicenseService;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.google.common.collect.ImmutableMap.of;
import static com.sonatype.clm.testing.functional.elements.CLM.CSS_SIDEBAR_CLOSED;
import static com.sonatype.clm.testing.functional.elements.CLM.CSS_SIDEBAR_OPEN;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;

public class MainHeaderTest
    extends AbstractFunctionalTest
{
  @Before
  public void before() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    //Clear the experimental feature flag after running the test
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));
    // logout if not already logged out
    hardreset();
  }

  @Test
  public void testLoggedInUserName() {
    MainHeader.userMenu().dropdownToggle().click();
    MainHeader.userMenu().userName().shouldBe(visible).shouldHave(text("Admin BuiltIn"));
  }

  @Test
  public void testUserMenuLinks() {
    UserMenu userMenu = MainHeader.userMenu();
    userMenu.changePassword().shouldNotBe(visible);
    userMenu.manageUserToken().shouldNotBe(visible);
    userMenu.userDetails().shouldNotBe(visible);
    userMenu.logout().shouldNotBe(visible);

    userMenu.dropdownToggle().shouldBe(visible).click();
    userMenu.changePassword().shouldBe(visible);
    userMenu.manageUserToken().shouldBe(visible);
    userMenu.userDetails().shouldBe(visible);
    userMenu.logout().shouldBe(visible);
  }

  @Test
  public void testLoginButton() {
    LoginModal loginModal = new LoginModal();
    logout();

    MainHeader.loginButton().shouldNotBe(visible);
    loginModal.vulnerabilityLookupLink().click();

    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    MainHeader.loginButton().shouldBe(visible);

    loginAsAdmin();
    MainHeader.loginButton().shouldNotBe(visible);

    logout();
    MainHeader.loginButton().shouldNotBe(visible);
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
    MainHeader.productVersion().shouldHave(text(productVersion));
  }

  @Test
  public void testSystemDropdowns() {
    refreshOrOpen(DashboardPage.urlToViolations());

    DashboardPage.filterToggle().shouldBe(visible).click();
    AgeFilter ageFilter = DashboardFilters.ageFilter();
    ageFilter.shouldBe(visible);
    ageFilter.twisty().click();
    ageFilter.past90days().shouldNotBe(selected).click();
    DashboardFilters.closeButton().shouldBe(CLM.DISABLED);
    DashboardPage.violationsView().results().mask().shouldBe(visible);

    UserMenu userMenu = MainHeader.userMenu();
    userMenu.dropdownToggle().click();
    userMenu.userDetails().shouldBe(visible);
    userMenu.logout().shouldBe(visible);

    SystemConfigMenu sysConfigMenu = MainHeader.systemConfigMenu();
    sysConfigMenu.dropdownToggle().click();
    sysConfigMenu.successMetrics().shouldBe(visible);

    eyesWatcher.eyesCheck("Top Nav Dropdown not hidden");

    HelpMenu helpMenu = MainHeader.helpMenu();
    helpMenu.dropdownToggle().click();
    helpMenu.supportLink().shouldBe(visible);
  }

  @Test
  public void testSidebar_DefaultsToOpen() {
    MainHeader.sidebar()
        .shouldBe(visible)
        .shouldHave(CSS_SIDEBAR_OPEN)
        .shouldNotHave(CSS_SIDEBAR_CLOSED);
  }

  @Test
  public void testSidebar_Toggles() {
    MainHeader.sidebar().shouldBe(visible).shouldHave(CSS_SIDEBAR_OPEN);
    eyesWatcher.eyesCheck("Left Nav Sidebar Open");
    MainHeader.toggleNavigationButton().shouldBe(visible).click();
    MainHeader.sidebar().shouldBe(visible).shouldHave(CSS_SIDEBAR_CLOSED);
    eyesWatcher.eyesCheck("Left Nav Sidebar Closed");
    MainHeader.toggleNavigationButton().shouldBe(visible).click();
    MainHeader.sidebar().shouldBe(visible).shouldHave(CSS_SIDEBAR_OPEN);
  }

  @Test
  public void testDashboardIcon_DashboardAvailable() {
    MainHeader.dashboardNavigationButton().shouldBe(visible);
  }

  @Test
  public void testDashboardIcon_DashboardNotAvailable() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();
    MainHeader.policiesNavigationButton().shouldBe(visible);
    MainHeader.dashboardNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testReportingIcon_ReportsListAvailable() {
    MainHeader.reportingNavigationButton().shouldBe(visible);
  }

  @Test
  public void testReportingIcon_ReportsListNotAvailable() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.REPORTS_LIST_DISABLED, "true");
    refresh();
    MainHeader.policiesNavigationButton().shouldBe(visible);
    MainHeader.reportingNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testNavigation_ToDashboard() {
    MainHeader.dashboardNavigationButton().click();
    waitUntilUrl(DashboardPage.url());
  }

  @Test
  public void testNavigation_ToReporting() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    MainHeader.reportingNavigationButton().click();
    waitUntilUrl(ReportListPage.url());
  }

  @Test
  public void testNavigation_ToPolicies() {
    MainHeader.policiesNavigationButton().click();
    waitUntilUrl(OwnerSummaryPage.urlToRootOrg());
  }

  @Test
  public void testNavigation_ToLabs() {
    MainHeader.labsNavigationButton().click();
    waitUntilUrl(SuccessMetricsReportListPage.url());
  }

  @Test
  public void testNavigation_ToVulnerabilityDetails() {
    MainHeader.vulnerabilityDetailsNavigationButton().click();
    waitUntilUrl(VulnerabilitySearchPage.url());
  }

  @Test
  public void testAdvancedSearchNavigationButton_HiddenByDefault() {
    MainHeader.advancedSearchNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testNavigation_ToAdvancedSearch() {
    new SystemConfigurationPropertyDAO().update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "true"));
    refresh();
    MainHeader.advancedSearchNavigationButton().shouldBe(visible).click();
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
      MainHeader.advancedSearchNavigationButton().shouldBe(visible).click();
      waitUntilUrl(AdvancedSearchPage.url());
    }
    finally {
      logout();
      refreshOrOpen(DashboardPage.url());
      loginAsAdmin();
    }
  }

  @Test
  public void testFirewallNavigationButton_HiddenByDefault() {
    MainHeader.firewallNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testFirewallNavigationButton_FeatureFlagDisabledHidden() {
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.RELEASE_INTEGRITY);
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));
    MainHeader.firewallNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testFirewallNavigationButton_FeaturesUnavailableHidden() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    MainHeader.firewallNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testNavigation_ToFirewall() {
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.RELEASE_INTEGRITY);
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    refresh();
    MainHeader.firewallNavigationButton().shouldBe(visible).click();
    waitUntilUrl(FirewallPage.url());
  }

  @Test
  public void testLegalNavigationButton_HiddenByDefault() {
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);
    refresh();
    MainHeader.legalNavigationButton().shouldBe(hidden);
  }

  @Test
  public void testLegalNavigationButton_FeatureAvailableAdmin() {
    MainHeader.legalNavigationButton().shouldBe(visible);
  }

  @Test
  public void testLegalNavigationButton_FeatureAvailableNonAdmin() {
    User user = tempEntity.newUser();
    refreshOrOpen(DashboardPage.url());
    logout();
    login(user.getUsername(), user.getPassword());
    MainHeader.legalNavigationButton().shouldBe(visible);
  }

  @Test
  public void testLegalNavigation_toLegalDashboard() throws Exception {
    MainHeader.legalNavigationButton().shouldBe(visible).click();
    waitUntilUrl(BaseUrl.resolvePageUrl("/legal/dashboard"));
  }
}
