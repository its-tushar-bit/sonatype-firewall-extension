/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.VulnerabilitySearchPage;
import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.service.InsightConfig;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.InputUtils.clearInput;

public class LoginTest
    extends AbstractFunctionalTest
{
  private final LoginModal loginModal = new LoginModal();

  private final VulnerabilitySearchPage vulnPage = new VulnerabilitySearchPage();

  private final InsightConfig insightConfig = testCLMServer.getCLMServer().getInstance(InsightConfig.class);

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
  public void testInitialLoginFormState_UnauthenticatedPagesDisabled() {
    Map<String, Boolean> currentFeatures = insightConfig.getFeatures();
    try {
      insightConfig.setFeatures(new HashMap<>());
      SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.setEnabled(false);

      refreshOrOpen(ReportListPage.url());
      loginModal.vulnerabilityLookupLink().shouldBe(hidden);
      loginModal.cancelButton().shouldBe(hidden);
      MainHeader.loginButton().shouldBe(hidden);
    }
    finally {
      insightConfig.setFeatures(currentFeatures);
    }
  }

  @Test
  public void testLoginFormStateInVulnerabilityLookupPage_UnauthenticatedPagesDisabled() {
    Map<String, Boolean> currentFeatures = insightConfig.getFeatures();
    try {
      insightConfig.setFeatures(new HashMap<>());
      SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.setEnabled(false);

      refreshOrOpen(VulnerabilitySearchPage.url());
      loginModal.vulnerabilityLookupLink().shouldBe(hidden);
      loginModal.cancelButton().shouldBe(hidden);
      MainHeader.loginButton().shouldBe(hidden);
    }
    finally {
      insightConfig.setFeatures(currentFeatures);
    }
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
  public void testInitialLoginFormState_SamlSso() {
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration();
    new SamlConfigurationDAO().update(samlConfiguration);
    testCLMServer.getCLMServer().getInstance(SamlDeploymentManager.class).updateFromConfiguration();

    refreshOrOpen(ReportListPage.url());

    loginModal.shouldBe(visible);
    loginModal.ssoButton().shouldBe(visible, focused);
    loginModal.loginButton().shouldHave(attribute("aria-disabled", "true"));
    eyesWatcher.eyesCheck();

    loginModal.username().setValue("u");
    loginModal.loginButton().shouldHave(attribute("aria-disabled", "true"));
    loginModal.password().setValue("p");
    loginModal.loginButton().shouldNotHave(attribute("aria-disabled"));
    clearInput(loginModal.username());
    loginModal.loginButton().shouldHave(attribute("aria-disabled", "true"));
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
  public void testErrorMessageClearOnNavigateToVulnerabilitySearchPage() {
    refreshOrOpen(ReportListPage.url());
    loginModal.shouldBe(visible);
    loginModal.username().setValue("unknown");
    loginModal.password().setValue("user");
    loginModal.loginButton().shouldBe(enabled).click();
    loginModal.shouldBe(visible);
    loginModal.errorMessage().shouldBe(visible).shouldHave(text("Invalid credentials"));

    loginModal.vulnerabilityLookupLink().shouldBe(visible).click();
    waitUntilUrl(VulnerabilitySearchPage.url());
    loginModal.shouldNotBe(visible);
    vulnPage.shouldBe(visible);

    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    loginModal.errorMessage().shouldNotBe(visible);
  }

  @Test
  public void testErrorMessageClearOnCancel() {
    refreshOrOpen(ReportListPage.url());
    loginModal.shouldBe(visible);
    loginModal.vulnerabilityLookupLink().shouldBe(visible).click();
    waitUntilUrl(VulnerabilitySearchPage.url());
    loginModal.shouldNotBe(visible);
    vulnPage.shouldBe(visible);

    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    loginModal.username().setValue("unknown");
    loginModal.password().setValue("user");
    loginModal.loginButton().shouldBe(enabled).click();
    loginModal.shouldBe(visible);
    loginModal.errorMessage().shouldBe(visible).shouldHave(text("Invalid credentials"));
    loginModal.cancelButton().shouldBe(visible).click();

    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    loginModal.errorMessage().shouldNotBe(visible);
  }

  @Test
  public void testAuthenticationSessionStateIsRememberedByCookie() {
    refreshOrOpen(OwnerSummaryPage.url());
    loginAsAdmin();
    OwnerSummaryPage.summaryTile().shouldBe(visible);
    refreshOrOpen(ReportListPage.url());
    ReportListPage.listContainer().shouldBe(visible);
    clearCookies();
    refreshOrOpen(ReportListPage.url());
    loginModal.shouldBe(visible);
  }

  @Test
  public void testLogout() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
    logout();
    SidebarNavigation.mainHeaderButtons().shouldBe(hidden);
    loginModal.shouldBe(visible);
  }

  @Test
  public void testNavigationWhileLoggedOut() {
    refreshOrOpen(ReportListPage.url());
    loginModal.shouldBe(visible);
    refreshOrOpen(OwnerSummaryPage.url());
    loginModal.shouldBe(visible);
    Selenide.back();
    loginModal.shouldBe(visible);
    Selenide.forward();
    loginModal.shouldBe(visible);
  }
}
