/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.TestCLMServer;

public class SimilarWaiverCreator
{
  private static final String SCAN_ID = "5e4dc847bd4ca679aeca6af41c87af5e";

  private final Application app;

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private final Consumer<String> refreshOrOpen;

  public SimilarWaiverCreator(
      URL zippedReport,
      Application app,
      TestCLMServer testCLMServer,
      Consumer<String> refreshOrOpen,
      String baseUrl) throws IOException
  {
    this.app = app;
    this.refreshOrOpen = refreshOrOpen;
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrl, work);
    evaluator.evaluatePolicy();
  }

  public void createSimilarWaiver() {
    refreshOrOpen.accept(ApplicationReportPage.url(app, SCAN_ID));
    WaiverApplierForReport.waiveReportRow(reportPage, 0);
  }
}
