/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.clm.testing.functional.pages.VulnerabilitySearchPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.product.license.ProductLicenseService;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
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
    // logout if not already logged out
    hardreset();
  }

  @Test
  public void testLoggedInUserName() {
    MainHeader.userMenu().dropdownToggle().click();
    MainHeader.userMenu().userName().shouldBe(visible).shouldHave(text("Admin BuiltIn"));
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
}
