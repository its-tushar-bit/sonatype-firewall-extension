/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.LogoutWarningModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.MainView;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.pages.ApplicationReportContainerPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;
import com.sonatype.clm.testing.functional.pages.ReportPage;
import com.sonatype.clm.testing.functional.pages.ReportPolicyPage;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;
import com.sonatype.clm.testing.functional.pages.WebhookEditPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import com.codeborne.selenide.Selenide;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

@Ignore // https://sonatype.atlassian.net/browse/CLM-30526
public class SessionTimeoutTest
    extends AbstractFunctionalTest
{
  private DefaultWebSessionManager sessionManager;

  private long oldSessionTimeout;

  @Before
  public void before() {
    refreshOrOpen(DashboardPage.url());

    sessionManager = testCLMServer.getCLMServer().getInstance(DefaultWebSessionManager.class);
    oldSessionTimeout = sessionManager.getGlobalSessionTimeout();
  }

  @After
  public void after() {
    sessionManager.setGlobalSessionTimeout(oldSessionTimeout);
    Selenide.clearBrowserCookies();
  }

  /**
   * Test that when the session expires (simulated by deleting the cookie), that the next authentication-requiring
   * HTTP request causes the page to reload back to the login screen
   */
  @Test
  public void testReloginPromptOnAjaxDetectedSessionExpiration() {
    refreshOrOpen(ProductLicensePage.url());
    loginAsAdmin();

    // wait for all REST requests to finish
    ProductLicensePage.fingerprint().shouldBe(visible);
    SidebarNavigation.labsNavigationButton().shouldBe(visible);
    SystemConfigMenu systemConfigMenu = MainHeader.systemConfigMenu();
    systemConfigMenu.dropdownToggle().shouldBe(visible);

    hardreset();

    // try to open the Webhooks page. Since the session cookie has been deleted this should trigger the session
    // timeout detection
    systemConfigMenu.dropdownToggle().shouldBe(visible).click();
    systemConfigMenu.webhooks().click();

    assertUiClearedAndLogBackIn();

    logout();

    // verify that after logging out properly, the login dialog is fully useable again
    LoginModal loginModal = new LoginModal();
    loginModal.shouldBe(visible);
    loginModal.username().shouldHave(value("")).shouldBe(enabled);
    loginModal.password().shouldHave(value("")).shouldBe(enabled);
  }

  /**
   * Test that the re-login works when triggered from within a report iframe
   */
  @Test
  public void testReloginPromptOnAjaxDetectedSessionExpirationInReport() throws IOException {
    String scanId = "306e0a923df34c64b836358182b1b902";

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    TestReportEvaluator evaluator = new TestReportEvaluator(app, scanId,
        ReportHelper.zipReport("/canned-reports/small-report", tempDir), baseUrlFromTest,
        new InsightWork(testCLMServer.getCLMServer().getConfiguration()));

    evaluator.evaluatePolicy();

    loginAsAdmin();
    refreshOrOpen(ApplicationReportContainerPage.url(app.getPublicId(), scanId));

    Selenide.switchTo().frame(ApplicationReportContainerPage.getIframe());
    ReportPage.licenseChart().shouldBe(visible);

    hardreset();

    // try to access another page of the report after the session is expired. This is expected to
    // bring up the re-login modal, and then after successful reauthentication, is expected to navigate to the
    // other page
    ReportPage.policyTabButton().click();

    // switch back to the parent frame in order to deal with the login dialog
    Selenide.switchTo().defaultContent();
    assertUiClearedAndLogBackIn();

    // ensure that after logging back in, the report page loaded correctly
    Selenide.switchTo().frame(ApplicationReportContainerPage.getIframe());
    ReportPage.summaryTabButton().shouldBe(visible);

    ReportPage.policyTabButton().click();

    // expire the session again in order to test the re-login for the CIP itself
    hardreset();

    // click a row. This triggers the re-login using different logic so it needs to be tested separately
    ReportPolicyPage.row(0).openCip();

    Selenide.switchTo().defaultContent();
    assertUiClearedAndLogBackIn();

    // ensure the report is loaded again. Since a full page refresh happened the state within the iframe is lost
    Selenide.switchTo().frame(ApplicationReportContainerPage.getIframe());
    ReportPage.summaryTabButton().shouldBe(visible);

    // cleanup
    Selenide.switchTo().defaultContent();
  }

  @Test
  public void testRefreshAfterServerTimeout() throws Exception {
    // set session timeout to 1 second
    sessionManager.setGlobalSessionTimeout(1000);

    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();

    Thread.sleep(1500);

    assertUiCleared();
  }

  @Test
  public void testRefreshAfterServerTimeoutWithCookieUpdate() throws Exception {
    SystemConfigMenu systemConfigMenu = MainHeader.systemConfigMenu();
    // set session timeout to 122 seconds
    sessionManager.setGlobalSessionTimeout(122000);

    // Current Time: 0; Timeout Time: N/A
    loginAsAdmin();
    Thread.sleep(1000);

    // Perform an interaction that will cause a server request
    // Current Time: 1000; Timeout Time: 121000; Modal Time (time at which the modal opens): 2000
    systemConfigMenu.dropdownToggle().shouldBe(visible).click();
    systemConfigMenu.webhooks().click();

    // wait until the original timeout for the modal is met and assert that it is not shown
    Thread.sleep(1500);
    // Current Time: 2500; Timeout Time: 119500; Modal Time: 500
    LogoutWarningModal logoutWarningModal = new LogoutWarningModal();
    logoutWarningModal.warningText().shouldNotBe(visible);

    // wait until under 2 minutes to session expiration so that modal shows up
    Thread.sleep(1000);
    // Current Time: 3500; Timeout Time: 118500; Modal Time: 0.
    logoutWarningModal.warningText().shouldBe(visible);
  }

  /**
   * Test that a session timeout causes an immediate page refresh even in situations where we would normally block
   * a page change due to dirty fields in the page
   */
  @Test
  public void testRefreshDespiteDirtyPage() throws Exception {
    SystemConfigMenu systemConfigMenu = MainHeader.systemConfigMenu();

    // set session timeout to 6 seconds
    sessionManager.setGlobalSessionTimeout(6000);

    loginAsAdmin();
    LogoutWarningModal logoutWarningModal = new LogoutWarningModal();
    logoutWarningModal.keepMeSignedInButton().shouldBe(visible).click();
    systemConfigMenu.dropdownToggle().shouldBe(visible).click();
    systemConfigMenu.webhooks().click();
    new WebhookConfigurationPage().newWebhook().shouldBe(visible).click();
    new WebhookEditPage().url().shouldBe(visible).setValue("test");

    Thread.sleep(9000);

    assertUiCleared();
  }

  @Test
  public void testLogoutWarningModal() throws Exception {
    // set session timeout to 4 seconds
    sessionManager.setGlobalSessionTimeout(4000);
    loginAsAdmin();

    LogoutWarningModal logoutWarningModal = new LogoutWarningModal();
    logoutWarningModal.keepMeSignedInButton().shouldBe(visible);
    logoutWarningModal.warningText().shouldHave(
        text("Due to 30 minutes of inactivity you will be logged out in 2 seconds."));

    Thread.sleep(4500);

    assertUiCleared();
  }

  @Test
  public void testLogoutWarningModal_ExtendSession() throws Exception {
    // set session timeout to 3 seconds
    sessionManager.setGlobalSessionTimeout(3000);

    // Current Time: 0; Timeout Time: N/A
    loginAsAdmin();
    Thread.sleep(1500);

    // Clicking the button should trigger an interaction that will cause a server request
    // Current Time: 1500; Timeout Time: 3000
    refresh(); //https://sonatype.atlassian.net/browse/CLM-30526
    LogoutWarningModal logoutWarningModal = new LogoutWarningModal();
    logoutWarningModal.keepMeSignedInButton().shouldBe(visible).click();

    // wait until after the initial timeout would've expired, but not after the timeout from the most recent
    // interaction would've expired
    Thread.sleep(2000);
    logoutWarningModal.warningText().shouldBe(visible);
    // wait until after the new timeout should expire
    Thread.sleep(3000);

    assertUiCleared();
  }

  private void assertUiCleared() {
    // ensure that the main UI is empty - we can't directly test that the page was
    // refreshed but this is close
    SidebarNavigation.mainHeaderButtons().shouldBe(hidden);
    MainView.uiView().$$("*").shouldHave(size(0));
  }

  private void assertUiClearedAndLogBackIn() {
    assertUiCleared();
    loginAsAdmin();
  }
}
