/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.security.SamlDeploymentManager;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class LoginTest
    extends AbstractFunctionalTest
{
  private final LoginModal loginModal = new LoginModal();

  @Before
  @After
  public void clearSamlDeployment() {
    testCLMServer.getCLMServer().getInstance(SamlDeploymentManager.class).updateFromConfiguration();
  }

  @After
  public void clearCookies() {
    Selenide.clearBrowserCookies();
  }

  @Test
  public void testInitialLoginFormState() {
    refreshOrOpen(ReportListPage.url());
    loginModal.shouldBe(visible);
    loginModal.ssoButton().shouldBe(hidden);
    loginModal.username().shouldBe(focused);
    loginModal.ssoButton().shouldBe(hidden);
    loginModal.loginButton().shouldBe(enabled);
    loginModal.cancelButton().shouldBe(hidden);
    loginModal.vulnerabilityLookupLink().shouldBe(visible);
  }

  @Test
  public void testValidCredentials() {
    refreshOrOpen(ReportListPage.url());
    loginModal.shouldBe(visible);
    loginModal.username().setValue("admin");
    loginModal.password().setValue("admin123");
    loginModal.loginButton().shouldBe(enabled).click();
    loginModal.shouldBe(hidden);
  }

  @Test
  public void testInvalidCredentials() {
    refreshOrOpen(ReportListPage.url());
    loginModal.shouldBe(visible);
    loginModal.username().setValue("unknown");
    loginModal.password().setValue("user");
    loginModal.loginButton().shouldBe(enabled).click();
    loginModal.shouldBe(visible);
    loginModal.errorMessage().shouldBe(visible).shouldHave(text("Invalid credentials"));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testLogout() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
    logout();
    SidebarNavigation.sidebarLinks().shouldBe(empty);
    loginModal.shouldBe(visible);
  }

}
