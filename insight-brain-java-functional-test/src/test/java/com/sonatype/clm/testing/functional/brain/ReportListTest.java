/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage.ReportListRow;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

public class ReportListTest
    extends AbstractFunctionalTest
{
  public static final String BUILD_SCAN_ID = "BUILD_SCAN_ID";

  public static final String STAGE_SCAN_ID = "STAGE_SCAN_ID";

  public static final String RELEASE_SCAN_ID = "RELEASE_SCAN_ID";

  public Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = new PolicyImportExport();

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

    // Build report
    TestReportEvaluator evaluatorBuild =
        new TestReportEvaluator(app, BUILD_SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluatorBuild.evaluatePolicy();

    // Stage Release report
    TestReportEvaluator stageBuild =
        new TestReportEvaluator(app, STAGE_SCAN_ID, zippedReport, Configuration.baseUrl, work, Stage.ID_STAGE_RELEASE);
    stageBuild.evaluatePolicy();

    // Release report
    TestReportEvaluator releaseBuild =
        new TestReportEvaluator(app, RELEASE_SCAN_ID, zippedReport, Configuration.baseUrl, work, Stage.ID_RELEASE);
    releaseBuild.evaluatePolicy();
  }

  @Test
  public void testReportLinks() {
    refreshOrOpen(ReportListPage.URL);
    ReportListRow firstRow = ReportListPage.firstRow();
    firstRow.shouldBe(visible);

    SelenideElement buildLink = firstRow.buildReportLink();
    SelenideElement stageReleaseLink = firstRow.stageReleaseReportLink();
    SelenideElement releaseLink = firstRow.releaseReportLink();

    buildLink.shouldBe(visible);
    stageReleaseLink.shouldBe(visible);
    releaseLink.shouldBe(visible);
    ApplicationReportPage reportPage = new ApplicationReportPage();

    buildLink.click();
    reportPage.shouldBe(visible);
    refreshOrOpen(ReportListPage.URL);

    stageReleaseLink.click();
    reportPage.shouldBe(visible);
    refreshOrOpen(ReportListPage.URL);

    releaseLink.click();
    reportPage.shouldBe(visible);
  }
}
