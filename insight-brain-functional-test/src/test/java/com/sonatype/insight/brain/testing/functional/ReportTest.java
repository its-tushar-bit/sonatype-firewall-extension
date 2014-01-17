/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional;

import java.io.File;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.testing.functional.ReportPage.Report;
import com.sonatype.insight.brain.testing.functional.ReportPage.ReportSummaryTab;

import com.yammer.dropwizard.testing.JsonHelpers;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ReportTest
    extends AbstractFunctionalTest
{

  private String appId = ReportTest.class.getSimpleName();

  private String scanId = "scan1234";

  @Before
  public void setup() throws Exception {
    // Create Application
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application app = new Application(appId, "asdf", null);
    applicationDAO.insert(app);

    // copy scan
    File sonatypeWork = getConfig().getSonatypeWork();
    final File saasReportFile1 = new File(new File(new File(new File(sonatypeWork, "report"), app.getId()), scanId), "report.zip");
    FileUtils.copyURLToFile(getClass().getResource("/ReportTest/report.zip"), saasReportFile1);
    // Trigger evaluation
    post(getEvalURL(appId, scanId), JsonHelpers.asJson(new Stage(Stage.ID_BUILD)), "admin", "admin123");

    // nuke session cookie from any prior tests
    driver.manage().deleteCookieNamed("JSESSIONID");
  }

  @After
  public void teardown() {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    applicationDAO.delete(applicationDAO.getByPublicId(appId));
  }

  @Test
  public void testReportLink() {
    driver.get(getUiLinksReportUrl(appId, scanId));
    PageFactory.initElements(driver, Login.class).doLogin("admin", "admin123");

    final ReportPage reportPage = PageFactory.initElements(driver, ReportPage.class);
    wait(10, ExpectedConditions.visibilityOf(reportPage.getReportFrame()));

    driver.switchTo().frame(reportPage.getReportFrame());
    wait(10, ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

    // Verify content of report
    waitForSummaryPage(reportPage.getReport());
    ReportSummaryTab summary = PageFactory.initElements(driver, Report.class).getSummary();
    Assert.assertEquals(28, summary.getComponentsIdentified());
    Assert.assertEquals(42, summary.getSecurityAlerts());
  }
  
  @Test
  public void testReportAuthentication() throws Exception {
    driver.get(getUiLinksReportUrl(appId, scanId));
    PageFactory.initElements(driver, Login.class).doLogin("admin", "admin123");

    final ReportPage reportPage = PageFactory.initElements(driver, ReportPage.class);
    wait(10, ExpectedConditions.visibilityOf(reportPage.getReportFrame()));

    driver.switchTo().frame(reportPage.getReportFrame());
    final Report report = reportPage.getReport();
    //make sure the summary page loads
    report.clickSummary();
    waitForSummaryPage(report);
    //then the policy page
    report.clickPolicy();
    waitForPolicyPage(report);
    //now back to summary page
    report.clickSummary();
    waitForSummaryPage(report);
    //now lets clear out cookies
    driver.manage().deleteCookieNamed("JSESSIONID");
    //now back to the policy page, authentication should be required
    report.clickPolicy();
    waitForLoginPage(report, true);
    //now lets login with bad password
    report.getLogin().getUsername().sendKeys("admin");
    report.getLogin().getPassword().sendKeys("admin1234");
    Assert.assertFalse(report.getLogin().getError().isDisplayed());
    report.getLogin().getLoginButton().click();
    waitForLoginError(report);
    //now lets login with real password
    report.getLogin().getUsername().clear();
    report.getLogin().getUsername().sendKeys("admin");
    report.getLogin().getPassword().clear();
    report.getLogin().getPassword().sendKeys("admin123");
    report.getLogin().getLoginButton().click();
    Assert.assertFalse(report.getLogin().getError().isDisplayed());
    //make sure login is gone
    waitForLoginPage(report, false);
    //make sure login dom has been reset properly
    Assert.assertEquals(report.getLogin().getUsername().getAttribute("value"), "");
    Assert.assertEquals(report.getLogin().getPassword().getAttribute("value"), "");
    waitForPolicyPage(report);
  }
  
  private void waitForLoginError(final Report report) {
    wait(10, new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(WebDriver driver) {
        return report.getLogin().getError().isDisplayed();
      }
    });
  }
  
  private void waitForSummaryPage(final Report report) {
    wait(10, new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(WebDriver driver) {
        return report.getSummary().isDisplayed();
      }
    });
  }
  
  private void waitForPolicyPage(final Report report) {
    wait(10, new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(WebDriver driver) {
        return report.getPolicy().isDisplayed();
      }
    });
  }
  
  private void waitForLoginPage(final Report report, final boolean displayed) {
    wait(10, new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(WebDriver driver) {
        return displayed == report.getLogin().isDisplayed();
      }
    });
  }

  private String getEvalURL(String appId, String scanId) {
    return getBaseUrl() + PolicyEvaluateResource.SERVICE_PATH.replace("{applicationPublicId}", appId) + "?scanId=" + scanId;
  }

  private String getUiLinksReportUrl(String appId, String scanId) {
    return getBaseUrl() + UserInterfaceLinksResource.SERVICE_PATH + "/"
        + UserInterfaceLinksResource.REPORT_PATH.replace("{applicationPublicId}", appId).replace("{scanId}", scanId);
  }
}
