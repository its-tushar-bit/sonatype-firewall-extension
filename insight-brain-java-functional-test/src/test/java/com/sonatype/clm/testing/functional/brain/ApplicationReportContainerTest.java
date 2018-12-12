/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationReportContainerPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Configuration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ApplicationReportContainerTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private Application app;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    app = tempEntity.newApplicationWithParent("ApplicationReportContainerTest", "ApplicationReportContainerTest");
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);

    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportContainerPage.url(app.getPublicId(), SCAN_ID));
  }

  @Test
  public void testPolicyCentricAppReportPreview() {
    eyesWatcher.eyesCheck();

    ApplicationReportContainerPage.policyCentricAppReportPreviewAlert().shouldBe(visible);
    ApplicationReportContainerPage.policyCentricAppReportPreviewLink().shouldBe(visible).click();

    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    new ApplicationReportPage().reportTitle().shouldHave(text("ApplicationReportContainerTest"));
  }
}
