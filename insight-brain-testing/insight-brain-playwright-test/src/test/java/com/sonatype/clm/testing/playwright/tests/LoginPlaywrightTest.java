/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.LoginPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.pages.SidebarComponentAssertions;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright test for the Login page.
 */
public class LoginPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String INVALID_USERNAME = "unknown";

  private static final String INVALID_PASSWORD = "user";

  private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";

  @After
  public void clearCookies() {
    playwrightHardreset();
  }

  @Test
  @Category(SanityTest.class)
  public void testInitialLoginFormState() {
    playwrightRefreshOrOpen(ReportListPage.url());

    LoginPage loginPage = new LoginPage();
    new LoginPageAssertions(loginPage).shouldBeVisible();
    assertThat(loginPage.ssoButton()).isHidden();
    assertThat(loginPage.usernameInput()).isFocused();
    assertThat(loginPage.loginButton()).isEnabled();
    assertThat(loginPage.cancelButton()).isHidden();
    assertThat(loginPage.vulnerabilityLookupLink()).isVisible();
  }

  @Test
  @Category(SanityTest.class)
  public void testValidCredentials() {
    playwrightRefreshOrOpen(ReportListPage.url());

    LoginPage loginPage = new LoginPage();
    loginPage.loginAsAdmin();
  }

  @Test
  @Category(SanityTest.class)
  public void testInvalidCredentials() {
    playwrightRefreshOrOpen(ReportListPage.url());

    LoginPage loginPage = new LoginPage();
    loginPage.attemptLogin(INVALID_USERNAME, INVALID_PASSWORD);

    new LoginPageAssertions(loginPage).shouldBeVisible();
    assertThat(loginPage.errorMessage()).isVisible();
    assertThat(loginPage.errorMessage()).containsText(INVALID_CREDENTIALS_MESSAGE);
  }

  @Test
  @Category(SanityTest.class)
  public void testLogout() {
    playwrightRefreshOrOpen(ReportListPage.url());
    playwrightLogin();
    playwrightLogout();

    SidebarComponent sidebar = new SidebarComponent();
    new SidebarComponentAssertions(sidebar).shouldHaveEmptyLinks();

    LoginPage loginPage = new LoginPage();
    new LoginPageAssertions(loginPage).shouldBeVisible();
  }
}
