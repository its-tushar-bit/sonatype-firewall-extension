/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginDialog;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.pages.ApplicationReportContainerPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ReportPage;
import com.sonatype.clm.testing.functional.pages.ReportPolicyPage;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.google.common.base.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public class SessionTimeoutTest
    extends AbstractFunctionalTest
{

  @Before
  public void before() {
    Selenide.open(DashboardPage.URL);
  }

  /**
   * Test that when the session expires (simulated by deleting the cookie), that the next authentication-requiring
   * HTTP request causes the login prompt to appear, with the username box already filled in and disabled.
   */
  @Test
  public void testReloginPromptOnSessionExpiration() {
    loginAsAdmin();
    hardreset();

    // try to open the Webhooks page. Since the session cookie has been deleted this should trigger the session
    // timeout detection
    SystemConfigMenu systemConfigMenu = new SystemConfigMenu();
    systemConfigMenu.menu().click();
    systemConfigMenu.webhooks().click();

    assertReloginDialog();
    logBackIn();

    // confirm that we got to the webhooks page after logging back in
    new WebhookConfigurationPage().newWebhook().shouldBe(visible);

    logout();

    // verify that after logging out properly, the login dialog is fully useable again
    LoginDialog.root().shouldBe(visible);
    LoginDialog.username().shouldHave(value(""));
    LoginDialog.username().shouldBe(enabled);
    LoginDialog.password().shouldHave(value(""));
    LoginDialog.password().shouldBe(enabled);
  }

  /**
   * Test that the relogin works when triggered from within a report iframe
   */
  @Test
  public void testReloginPromptInReport() throws IOException {
    WebDriver driver = WebDriverRunner.getWebDriver();

    String scanId = "306e0a923df34c64b836358182b1b902";

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    TestReportEvaluator evaluator = new TestReportEvaluator(app, scanId,
        getClass().getResource("/canned-reports/small-report.zip"), Configuration.baseUrl,
        new InsightWork(testCLMServer.getCLMServer().getConfiguration()));

    evaluator.evaluatePolicy();

    loginAsAdmin();
    refreshOrOpen(ApplicationReportContainerPage.url(app.getPublicId(), scanId));

    driver.switchTo().frame(ApplicationReportContainerPage.getIframe());

    // expire the session, but only after the iframe document has fully loaded. Otherwise there is a race condition
    // where some of the css and js for the iframe won't load. To determine when the scripts have executed, we
    // wait until jquery ($) is defined
    Selenide.Wait() //
        .withTimeout(2, SECONDS) //
        .pollingEvery(100, MILLISECONDS) //
        .withMessage("jQuery failed to load") //
        .until((Predicate<WebDriver>) (d -> !((JavascriptExecutor) d).executeScript("return typeof $;")
            .equals("undefined")));

    hardreset();

    // try to access another page of the report after the session is expired. This is expected to
    // bring up the relogin modal, and then after successful reauthentication, is expected to navigate to the
    // other page
    ReportPage.policyTabButton().click();

    // switch back to the parent frame in order to deal with the login dialog
    driver.switchTo().defaultContent();
    assertReloginDialog();
    logBackIn();

    // ensure that after logging back in, the report page loaded correctly
    driver.switchTo().frame(ApplicationReportContainerPage.getIframe());
    ReportPolicyPage.summaryView().shouldBe(visible);
    ReportPolicyPage.row(0).coordinates().shouldHave(text("ch.qos.logback : logback-access : 0.6"));

    // cleanup
    driver.switchTo().defaultContent();
  }

  private void assertReloginDialog() {
    // verify that username is pre-filled and disabled
    LoginDialog.root().shouldBe(visible);
    LoginDialog.username().shouldHave(value("admin"));
    LoginDialog.username().shouldBe(disabled);
    LoginDialog.password().shouldHave(value(""));
    LoginDialog.password().shouldBe(enabled);
  }

  private void logBackIn() {
    LoginDialog.password().setValue("admin123");
    LoginDialog.loginButton().click();
    LoginDialog.root().shouldNotBe(visible);
  }
}
