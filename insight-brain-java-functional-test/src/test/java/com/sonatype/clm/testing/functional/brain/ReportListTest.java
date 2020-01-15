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
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQThreatIndicators;
import com.sonatype.clm.testing.functional.pages.ReportListPage.ReportListRow;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exactText;
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
    URL zippedLargeReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    URL zippedSmallReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

    // Build report
    TestReportEvaluator evaluatorBuild = new TestReportEvaluator(app, BUILD_SCAN_ID, zippedLargeReport,
        Configuration.baseUrl, work);
    evaluatorBuild.evaluatePolicy();

    // Stage Release report
    TestReportEvaluator stageBuild = new TestReportEvaluator(app, STAGE_SCAN_ID, zippedSmallReport,
        Configuration.baseUrl, work, Stage.ID_STAGE_RELEASE);
    stageBuild.evaluatePolicy();

    refreshOrOpen(ReportListPage.URL);
  }

  @Test
  public void testReportLinks() {
    ReportListRow firstRow = ReportListPage.firstRow();
    firstRow.shouldBe(visible);

    SelenideElement buildLink = firstRow.buildReportLink();
    SelenideElement stageReleaseLink = firstRow.stageReleaseReportLink();
    SelenideElement releaseLink = firstRow.releaseReportLink();

    buildLink.shouldBe(visible);
    stageReleaseLink.shouldBe(visible);
    releaseLink.shouldNotBe(visible);
    ApplicationReportPage reportPage = new ApplicationReportPage();

    buildLink.click();
    reportPage.shouldBe(visible);
    refreshOrOpen(ReportListPage.URL);

    stageReleaseLink.click();
    reportPage.shouldBe(visible);
  }

  @Test
  public void testChiclets() {
    ReportListRow firstRow = ReportListPage.firstRow();
    firstRow.shouldBe(visible);

    IQThreatIndicators buildThreatIndicators = firstRow.buildReportThreatIndicators();
    buildThreatIndicators.critical().shouldHave(exactText("22"));
    buildThreatIndicators.severe().shouldHave(exactText("39"));
    buildThreatIndicators.moderate().shouldHave(exactText("4"));

    IQThreatIndicators stageReleaseThreatIndicators = firstRow.stageReleaseReportThreatIndicators();
    stageReleaseThreatIndicators.critical().shouldNotBe(visible);
    stageReleaseThreatIndicators.severe().shouldNotBe(visible);
    stageReleaseThreatIndicators.moderate().shouldHave(exactText("1"));

    IQThreatIndicators releaseThreatIndicators = firstRow.releaseReportThreatIndicators();
    releaseThreatIndicators.shouldNotBe(visible);
  }
}
