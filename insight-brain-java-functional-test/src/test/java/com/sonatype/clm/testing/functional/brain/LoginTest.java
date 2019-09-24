/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.security.SamlDeploymentManager;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class LoginTest
    extends AbstractFunctionalTest
{
  private LoginModal loginModal = new LoginModal();

  @After
  public void clearCookiesAndSamlDeployment() {
    Selenide.clearBrowserCookies();
    testCLMServer.getCLMServer().getInstance(SamlDeploymentManager.class).updateFromConfiguration();
  }

  @Test
  public void testInitialLoginFormState() {
    refreshOrOpen(ReportListPage.URL);
    loginModal.shouldBe(visible);
    loginModal.ssoText().shouldBe(hidden);
    loginModal.username().shouldBe(focused);
    loginModal.ssoButton().shouldBe(hidden);
    loginModal.loginButton().shouldBe(enabled);
  }

  @Test
  public void testInitialLoginFormState_SamlSso() {
    tempEntity.newSamlConfiguration();
    testCLMServer.getCLMServer().getInstance(SamlDeploymentManager.class).updateFromConfiguration();

    refreshOrOpen(ReportListPage.URL);

    loginModal.shouldBe(visible);
    loginModal.ssoText().shouldBe(visible);
    loginModal.username().shouldBe(focused);
    loginModal.ssoButton().shouldBe(enabled);
    loginModal.loginButton().shouldBe(disabled);
    eyesWatcher.eyesCheck();

    loginModal.username().setValue("u");
    loginModal.loginButton().shouldBe(disabled);
    loginModal.password().setValue("p");
    loginModal.loginButton().shouldBe(enabled);
    loginModal.username().clear();
    loginModal.loginButton().shouldBe(disabled);
  }

  @Test
  public void testValidCredentials() {
    refreshOrOpen(ReportListPage.URL);
    loginModal.shouldBe(visible);
    loginModal.username().setValue("admin");
    loginModal.password().setValue("admin123");
    loginModal.loginButton().shouldBe(enabled).click();
    loginModal.shouldBe(hidden);
  }

  @Test
  public void testInvalidCredentials() {
    refreshOrOpen(ReportListPage.URL);
    loginModal.shouldBe(visible);
    loginModal.username().setValue("unknown");
    loginModal.password().setValue("user");
    loginModal.loginButton().shouldBe(enabled).click();
    loginModal.shouldBe(visible);
    loginModal.errorMessage().shouldBe(visible).shouldHave(text("Invalid credentials"));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testAuthenticationSessionStateIsRememberedByCookie() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
    OwnerSummaryPage.summaryTile().shouldBe(visible);
    refreshOrOpen(ReportListPage.URL);
    ReportListPage.listContainer().shouldBe(visible);
    clearCookiesAndSamlDeployment();
    refreshOrOpen(ReportListPage.URL);
    loginModal.shouldBe(visible);
  }

  @Test
  public void testLogout() {
    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
    logout();
    MainHeader.mainHeaderButtons().shouldBe(hidden);
    loginModal.shouldBe(visible);
    // take focus off the input to prevent blinking cursor
    loginModal.header().click();
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testNavigationWhileLoggedOut() {
    refreshOrOpen(ReportListPage.URL);
    loginModal.shouldBe(visible);
    refreshOrOpen(OrganizationManagementPage.URL);
    loginModal.shouldBe(visible);
    Selenide.back();
    loginModal.shouldBe(visible);
    Selenide.forward();
    loginModal.shouldBe(visible);
  }
}
