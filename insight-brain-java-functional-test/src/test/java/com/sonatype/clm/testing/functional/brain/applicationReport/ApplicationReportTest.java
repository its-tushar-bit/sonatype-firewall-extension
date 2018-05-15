/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationReport;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ApplicationReportTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    app = tempEntity.newApplicationWithParent("ApplicationReportTest", "ApplicationReportTest");
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work).evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testNavigation() {
    reportPage.shouldBe(visible);
    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.reportDate().shouldHave(text(DateTime.now().toString("yyyy-MM-dd")));
    reportPage.optionsDropdown().shouldBe(visible).menu().shouldNotBe(visible);
  }

  @Test
  public void testOptionsMenu() {
    IQDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.shouldBe(visible).menu().shouldNotBe(visible);
    optionsDropdown.button().shouldHave(text("Options")).click();
    optionsDropdown.menu().shouldBe(visible).entries()
        .shouldHave(texts("Re-Evaluate Report", "Generate PDF", "View raw data"));
    eyesWatcher.eyesCheck();
  }

}
