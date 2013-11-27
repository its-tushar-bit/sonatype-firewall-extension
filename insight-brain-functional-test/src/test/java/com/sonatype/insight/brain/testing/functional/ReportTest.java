/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.testing.functional.ReportPage.ReportSummaryPage;

import com.yammer.dropwizard.testing.JsonHelpers;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.PageFactory;
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
    ReportSummaryPage summary = PageFactory.initElements(driver, Report.class).getSummary();
    Assert.assertEquals(28, summary.getComponentsIdentified());
    Assert.assertEquals(36, summary.getSecurityAlerts());
  }

  private static String getEvalURL(String appId, String scanId) {
    return getUrl() + PolicyEvaluateResource.SERVICE_PATH.replace("{applicationPublicId}", appId) + "?scanId=" + scanId;
  }

  private static String getUiLinksReportUrl(String appId, String scanId) {
    return getUrl() + UserInterfaceLinksResource.SERVICE_PATH + "/"
        + UserInterfaceLinksResource.REPORT_PATH.replace("{applicationPublicId}", appId).replace("{scanId}", scanId);
  }
}
