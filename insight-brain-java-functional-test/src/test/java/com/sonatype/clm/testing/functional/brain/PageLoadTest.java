/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.clm.testing.functional.pages.VulnerabilitySearchPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Selenide;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

/**
 * This class tests the various situations in which the page load and login might happen, including the license not
 * being installed, access to pages that don't require authentication, etc
 */
public class PageLoadTest
    extends AbstractFunctionalTest
{
  @Before
  public void setup() {
    // ensure we are not logged in
    hardreset();
  }

  @Test
  public void testIndexHtml() {
    refreshOrOpen(BaseUrl.resolvePageUrl(""));
    loginAsAdmin();
    waitUntilUrl(DashboardPage.URL);
    DashboardPage.dashboardContainer().shouldBe(visible);
  }

  @Test
  public void testLoginModalVulnerabilitiesLink() {
    refreshOrOpen(BaseUrl.resolvePageUrl(""));
    LoginModal loginModal = new LoginModal();
    loginModal.cancelButton().shouldNotBe(visible);
    loginModal.vulnerabilityLookupText().shouldBe(visible);
    loginModal.vulnerabilityLookupLink().shouldBe(visible).click();

    waitUntilUrl(VulnerabilitySearchPage.url());
    loginModal.shouldNotBe(visible);
    VulnerabilitySearchPage vulnPage = new VulnerabilitySearchPage();
    vulnPage.shouldBe(visible);

    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    loginModal.cancelButton().shouldBe(visible);
    loginModal.vulnerabilityLookupText().shouldNotBe(visible);
    vulnPage.shouldBe(visible);
    loginAsAdmin();

    // wait a bit to ensure that that the page isn't redirecting somewhere else (like the dashboard)
    Selenide.sleep(1000);
    MainHeader.loginButton().shouldNotBe(visible);
    loginModal.shouldNotBe(visible);
    vulnPage.shouldBe(visible);
  }

  @Test
  public void testLoginModalVulnerabilitiesLink_FromDashboard() {
    refreshOrOpen(DashboardPage.URL);
    LoginModal loginModal = new LoginModal();
    loginModal.cancelButton().shouldNotBe(visible);
    loginModal.vulnerabilityLookupText().shouldBe(visible);
    loginModal.vulnerabilityLookupLink().shouldBe(visible).click();

    waitUntilUrl(VulnerabilitySearchPage.url());
    loginModal.shouldNotBe(visible);
    VulnerabilitySearchPage vulnPage = new VulnerabilitySearchPage();
    vulnPage.shouldBe(visible);

    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    loginModal.cancelButton().shouldBe(visible);
    loginModal.vulnerabilityLookupText().shouldNotBe(visible);
    vulnPage.shouldBe(visible);
    loginAsAdmin();

    // wait a bit to ensure that that the page isn't redirecting somewhere else (like the dashboard)
    Selenide.sleep(1000);
    MainHeader.loginButton().shouldNotBe(visible);
    loginModal.shouldNotBe(visible);
    vulnPage.shouldBe(visible);
  }

  @Test
  public void testLoginModalCancelFromVulnerabilitiesPage() {
    refreshOrOpen(DashboardPage.URL);
    LoginModal loginModal = new LoginModal();
    loginModal.cancelButton().shouldNotBe(visible);
    loginModal.vulnerabilityLookupText().shouldBe(visible);
    loginModal.vulnerabilityLookupLink().shouldBe(visible).click();

    waitUntilUrl(VulnerabilitySearchPage.url());
    loginModal.shouldNotBe(visible);
    VulnerabilitySearchPage vulnPage = new VulnerabilitySearchPage();
    vulnPage.shouldBe(visible);

    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    loginModal.vulnerabilityLookupText().shouldNotBe(visible);
    loginModal.cancelButton().shouldBe(visible).click();
    vulnPage.shouldBe(visible);

    // wait a bit to ensure that that the page isn't redirecting somewhere else (like the dashboard)
    Selenide.sleep(1000);
    MainHeader.loginButton().shouldBe(visible);
    loginModal.shouldNotBe(visible);
    vulnPage.shouldBe(visible);
  }

  @Test
  public void testLoadUnauthenticatedPage() {
    refreshOrOpen(VulnerabilitySearchPage.url());

    LoginModal loginModal = new LoginModal();
    loginModal.shouldNotBe(visible);
    VulnerabilitySearchPage vulnPage = new VulnerabilitySearchPage();
    vulnPage.shouldBe(visible);

    // ensure that logging in after going straight to an unauth page works
    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    loginModal.cancelButton().shouldBe(visible);
    loginModal.vulnerabilityLookupText().shouldNotBe(visible);
    vulnPage.shouldBe(visible);
    loginAsAdmin();

    // wait a bit to ensure that that the page isn't redirecting somewhere else (like the dashboard)
    Selenide.sleep(1000);
    MainHeader.loginButton().shouldNotBe(visible);
    loginModal.shouldNotBe(visible);
    vulnPage.shouldBe(visible);
  }

  @Test
  public void testLoadAuthPageAfterUnauthPage() {
    refreshOrOpen(VulnerabilitySearchPage.url());

    LoginModal loginModal = new LoginModal();
    loginModal.shouldNotBe(visible);
    VulnerabilitySearchPage vulnPage = new VulnerabilitySearchPage();
    vulnPage.shouldBe(visible);

    refreshOrOpen(DashboardPage.URL);
    loginModal.shouldBe(visible);
    vulnPage.shouldBe(visible);

    loginAsAdmin();
    loginModal.shouldNotBe(visible);
    vulnPage.shouldNotBe(visible);
    DashboardPage.dashboardContainer().shouldBe(visible);
  }

  @Test
  public void testLoadUnauthPageWhileOnLogin() {
    refreshOrOpen(DashboardPage.URL);

    LoginModal loginModal = new LoginModal();
    loginModal.shouldBe(visible);
    refreshOrOpen(VulnerabilitySearchPage.url());
    loginModal.shouldNotBe(visible);
    VulnerabilitySearchPage vulnPage = new VulnerabilitySearchPage();
    vulnPage.shouldBe(visible);
  }

  @Test
  public void testLoadNonDefaultAuthPage() {
    refreshOrOpen(SuccessMetricsReportListPage.URL);
    LoginModal loginModal = new LoginModal();
    loginModal.shouldBe(visible);
    loginAsAdmin();

    // Should go to the page specified, not default to the dashboard
    new SuccessMetricsReportListPage().shouldBe(visible);
  }

  @Test
  public void testLoadIndexHtml_NoDashboard() throws Exception {
    setMissingFeature(LicensedFeature.DASHBOARD);

    refreshOrOpen(BaseUrl.resolvePageUrl(""));
    loginAsAdmin();
    waitUntilUrl(ReportListPage.URL);
    ReportListPage.listContainer().shouldBe(visible);
  }

  @Test
  public void testLoadIndexHtml_NoLicense() throws Exception {
    uninstallLicense();

    refreshOrOpen(BaseUrl.resolvePageUrl(""));
    loginAsAdmin();
    waitUntilUrl(ProductLicensePage.url());
    ProductLicensePage.installLicenseBtn().shouldBe(visible);

    refreshOrOpen(DashboardPage.URL);
    Selenide.sleep(1000);

    // no effect - navigating to other pages not allowed
    ProductLicensePage.installLicenseBtn().shouldBe(visible);

    refreshOrOpen(VulnerabilitySearchPage.url());
    Selenide.sleep(1000);

    // no effect - navigating to unauthenticated pages not allowed either
    ProductLicensePage.installLicenseBtn().shouldBe(visible);
  }

  @Test
  public void testLoadNonDefaultAuthPage_NoLicense() throws Exception {
    uninstallLicense();

    refreshOrOpen(SuccessMetricsReportListPage.URL);
    LoginModal loginModal = new LoginModal();
    loginModal.shouldBe(visible);

    // no link to vuln lookup page before license is installed
    loginModal.vulnerabilityLookupLink().shouldNotBe(visible);
    loginAsAdmin();

    // should go to Product License Page
    waitUntilUrl(ProductLicensePage.url());
  }

  @Test
  public void testLoadUnauthenticatedPage_NoLicense() throws Exception {
    uninstallLicense();

    refreshOrOpen(VulnerabilitySearchPage.url());

    LoginModal loginModal = new LoginModal();

    // login required even though it is an unauthenticated page
    loginModal.shouldBe(visible);
    loginModal.vulnerabilityLookupLink().shouldNotBe(visible);
    loginAsAdmin();

    // should go to Product License Page
    waitUntilUrl(ProductLicensePage.url());
  }
}
