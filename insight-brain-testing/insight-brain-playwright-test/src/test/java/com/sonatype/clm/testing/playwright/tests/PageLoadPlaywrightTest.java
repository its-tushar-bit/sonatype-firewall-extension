/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.LoginPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ProductLicensePage;
import com.sonatype.clm.testing.playwright.pages.ProductLicensePageAssertions;
import com.sonatype.clm.testing.playwright.pages.VulnerabilitySearchPage;
import com.sonatype.clm.testing.playwright.pages.VulnerabilitySearchPageAssertions;

import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for page loading, login, and license scenarios.
 */
public class PageLoadPlaywrightTest
    extends AbstractIqUiTest
{

  @Test
  @Category(SanityTest.class)
  public void testIndexHtml() {
    playwrightOpenAndWaitForVisible(LoginPage.rootUrl(), new LoginPage().modal());
    playwrightLogin();

    playwrightRefreshOrOpen(DashboardPage.url());

    DashboardPage dashboard = new DashboardPage();
    assertThat(dashboard.dashboardContainer()).isVisible();
  }

  @Test
  @Category(SanityTest.class)
  public void testLoadUnauthenticatedPage() {
    playwrightOpenAndWaitForVisible(VulnerabilitySearchPage.url(), new VulnerabilitySearchPage().container());

    LoginPage loginPage = new LoginPage();
    assertThat(loginPage.modal()).isHidden();

    VulnerabilitySearchPage vulnPage = new VulnerabilitySearchPage();
    VulnerabilitySearchPageAssertions vulnAssertions = new VulnerabilitySearchPageAssertions(vulnPage);
    vulnAssertions.shouldBeVisible();

    HeaderComponent header = new HeaderComponent();
    assertThat(header.loginButton()).isVisible();
    header.loginButton().click();

    new LoginPageAssertions(loginPage).shouldBeVisible();
    assertThat(loginPage.cancelButton()).isVisible();
    assertThat(loginPage.vulnerabilityLookupText()).isHidden();
    vulnAssertions.shouldBeVisible();

    loginPage.loginAsAdmin();
    assertThat(header.loginButton()).isHidden();
    assertThat(loginPage.modal()).isHidden();
    vulnAssertions.shouldBeVisible();
  }

  @Test
  @Category(SanityTest.class)
  public void testLoadIndexHtml_NoLicense() {
    uninstallLicense();

    playwrightOpenAndWaitForVisible(LoginPage.rootUrl(), new LoginPage().modal());
    playwrightLogin();

    ProductLicensePage licensePage = new ProductLicensePage();
    new ProductLicensePageAssertions(licensePage).shouldShowInstallButton();

    playwrightRefreshOrOpen(DashboardPage.url());
    new ProductLicensePageAssertions(licensePage).shouldShowInstallButton();

    playwrightRefreshOrOpen(VulnerabilitySearchPage.url());
    new ProductLicensePageAssertions(licensePage).shouldShowInstallButton();
  }
}
