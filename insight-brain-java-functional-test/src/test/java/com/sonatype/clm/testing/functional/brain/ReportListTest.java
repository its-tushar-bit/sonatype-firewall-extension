/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQThreatIndicators;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage.ReportListRow;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class ReportListTest
    extends AbstractFunctionalTest
{
  public static final String BUILD_SCAN_ID = "BUILD_SCAN_ID";

  public static final String STAGE_SCAN_ID = "STAGE_SCAN_ID";

  private static final String CANNED_LARGE_REPORT_URI = "/canned-reports/large-report";

  private static final String CANNED_SMALL_REPORT_URI = "/canned-reports/small-report";

  public Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization("ApplicationReportTestOrgWithAReallyLongName");
    policyImportExport.importOrganization(org, referencePolicies);
    tempEntity.newUser("user1", "reallylongfirst", "even longer last name junior senior", "a@a.com");
    app = tempEntity.newApplication("ApplicationReportTestWithAReallyLongName",
        "ApplicationReportTestWithAReallyLongName", org.getId(), "user1");

    // Build report
    evaluatePolicy(BUILD_SCAN_ID, CANNED_LARGE_REPORT_URI, Stage.ID_BUILD);

    // Stage Release report
    evaluatePolicy(STAGE_SCAN_ID, CANNED_SMALL_REPORT_URI, Stage.ID_STAGE_RELEASE);

    refreshOrOpen(ReportListPage.url());
  }

  private void evaluatePolicy(String scanId, String reportDir, String stageId) throws IOException {
    URL zippedReport = ReportHelper.zipReport(reportDir, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

    new TestReportEvaluator(app, scanId, zippedReport, baseUrlFromTest, work, stageId)
        .evaluatePolicy();
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
    refreshOrOpen(ReportListPage.url());

    stageReleaseLink.click();
    reportPage.shouldBe(visible);
  }

  @Test
  public void testChiclets() {
    ReportListRow firstRow = ReportListPage.firstRow();
    firstRow.shouldBe(visible);

    IQThreatIndicators buildThreatIndicators = firstRow.buildReportThreatIndicators();
    buildThreatIndicators.critical().shouldHave(exactText("Critical 22"));
    buildThreatIndicators.severe().shouldHave(exactText("Severe 39"));
    buildThreatIndicators.moderate().shouldHave(exactText("Moderate 4"));

    IQThreatIndicators stageReleaseThreatIndicators = firstRow.stageReleaseReportThreatIndicators();
    stageReleaseThreatIndicators.critical().shouldHave(exactText("Critical 0"));
    stageReleaseThreatIndicators.severe().shouldHave(exactText("Severe 0"));
    stageReleaseThreatIndicators.moderate().shouldHave(exactText("Moderate 1"));

    IQThreatIndicators releaseThreatIndicators = firstRow.releaseReportThreatIndicators();
    releaseThreatIndicators.shouldNotBe(visible);
  }

  @Test
  public void testHeadersOrder() {
    ElementsCollection tableHeaders = ReportListPage.tableHeaders();

    List<String> headerNames = new ArrayList<>();
    for (SelenideElement tableHeader : tableHeaders) {
      headerNames.add(tableHeader.getText());
    }

    List<String> expectedHeaderNames = new ArrayList<>();
    expectedHeaderNames.add("APPLICATION");
    expectedHeaderNames.add("ORGANIZATION");
    expectedHeaderNames.add("SOURCE");
    expectedHeaderNames.add("BUILD");
    expectedHeaderNames.add("STAGE RELEASE");
    expectedHeaderNames.add("RELEASE");

    assertThat(headerNames).isEqualTo(expectedHeaderNames);
  }
}
