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
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ExpandedCoverageReportPage;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Selenide;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

public class ExpandedCoverageReportTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    URL zippedReport = ReportHelper.zipReport("/canned-reports/report-expanded_coverage", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    FileUtils.copyURLToFile(zippedReport, work.getReportFile(app.getId(), SCAN_ID));

    refreshOrOpen(ApplicationReportContainerPage.url(app.getPublicId(), SCAN_ID));
  }

  @Test
  public void testReportPresent() {
    ApplicationReportContainerPage.getIframe().shouldBe(visible);
    Selenide.switchTo().frame(ApplicationReportContainerPage.getIframe());

    ExpandedCoverageReportPage.componentTabButton().shouldBe(visible);
  }
}
