/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.utils.TestCredentials;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the IQ Server login modal.
 *
 * <p>
 * Locators follow the same accessibility queries as {@code LoginModal.jestspec.jsx}
 * ({@code getByRole('dialog')}, {@code getByLabel('Username')}, etc.).
 */
public class LoginPage
    extends BasePage
{
  public LoginPage() {
    super();
  }

  public static String rootUrl() {
    return "/assets/index.html";
  }

  public Locator modal() {
    // NxModal does not set aria-labelledby in this RSC version — use the stable id.
    return locator("#iq-login-modal");
  }

  public Locator usernameInput() {
    return byLabel("Username");
  }

  public Locator passwordInput() {
    return byLabel("Password");
  }

  public Locator loginButton() {
    // byRole(BUTTON,"Sign in") at page level also matches the header "Sign in" button —
    // strict-mode violation. Scope to the login modal to target only the form's submit button.
    return modal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Sign in"));
  }

  public Locator cancelButton() {
    return modal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"));
  }

  public Locator ssoButton() {
    return locator("#iq-login-modal-sso-button");
  }

  public Locator errorMessage() {
    // NxStatefulForm renders submitError via NxErrorAlert which has role="alert", not "status".
    // Scope to the login modal to avoid matching other alerts on the page.
    return locator("#iq-login-modal").getByRole(AriaRole.ALERT);
  }

  public Locator header() {
    return locator(".nx-modal-header");
  }

  public Locator systemNotice() {
    return locator(".iq-login-modal-system-notice");
  }

  public Locator vulnerabilityLookupText() {
    return byText("Look up a vulnerability without signing in at");
  }

  public Locator vulnerabilityLookupLink() {
    return byRole(AriaRole.LINK, "Vulnerability Lookup");
  }

  /**
   * Admin username from {@link TestCredentials#ADMIN_USERNAME}
   * (honors the {@code IQ_ADMIN_USERNAME} system-property override).
   */
  private static String defaultAdminUsername() {
    return TestCredentials.ADMIN_USERNAME;
  }

  /**
   * Admin password from {@link TestCredentials#ADMIN_PASSWORD}
   * (honors the {@code IQ_ADMIN_PASSWORD} system-property override).
   */
  private static String defaultAdminPassword() {
    return TestCredentials.ADMIN_PASSWORD;
  }

  /**
   * Perform a full login flow: fill credentials, click login, wait for modal to dismiss.
   */
  public void loginAs(String username, String password) {
    assertThat(modal())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
    usernameInput().fill(username);
    passwordInput().fill(password);
    loginButton().click();

    waitForVisibleThenHidden(".nx-submit-mask");

    assertThat(modal())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void loginAsAdmin() {
    loginAs(defaultAdminUsername(), defaultAdminPassword());
  }

  /**
   * Submit credentials without waiting for success (modal may stay open), e.g. invalid login.
   */
  public void attemptLogin(String username, String password) {
    assertThat(modal())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
    usernameInput().fill(username);
    passwordInput().fill(password);
    loginButton().click();
  }

  public void clickVulnerabilityLookupLink() {
    vulnerabilityLookupLink().click();
  }

}
