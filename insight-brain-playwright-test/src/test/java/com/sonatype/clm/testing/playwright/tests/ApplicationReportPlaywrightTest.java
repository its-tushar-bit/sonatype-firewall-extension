/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HdsMockServer;
import com.sonatype.clm.testing.playwright.categories.SanityTest;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApplicationReportPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String REPORT_DIR = "/canned-reports/large-report";

  private static final String ORGANIZATION_NAME_PREFIX = "AppReportTestOrg";

  private static final String APPLICATION_NAME_PREFIX = "AppReportTest";

  private static final String EXPECTED_THREAT_CRITICAL = "22";

  private static final String EXPECTED_THREAT_SEVERE = "39";

  private static final String EXPECTED_THREAT_MODERATE = "4";

  private static final String EXPECTED_VIOLATIONS_CAPTION = "65 VIOLATIONS";

  private static final String EXPECTED_VIOLATIONS_SUB_CAPTION = "Affecting 27 components";

  private static final String EXPECTED_COVERAGE_CAPTION = "64 COMPONENTS";

  private static final String EXPECTED_COVERAGE_SUB_CAPTION = "98% of all components identified";

  private static final int EXPECTED_VIOLATION_ROW_COUNT = 65;

  private static final int EXPECTED_TOTAL_ROW_COUNT = 103;

  private static final String COMPONENT_FILTER_TERM = "commons-fileupload";

  private static final int EXPECTED_FILTERED_VIOLATION_ROW_COUNT = 1;

  private static final int EXPECTED_FILTERED_TOTAL_ROW_COUNT = 6;

  private Application app;

  private String appName;

  @Before
  public void seedAndOpen() throws IOException {
    seedOrgAppAndPolicies();
    evaluateCannedReport();
    openReportAndLogin();
  }

  private void seedOrgAppAndPolicies() throws IOException {
    PolicyExportResult referencePolicies = JsonUtils.parse(
        getClass().getResource("/reference-policies-v3.json").openStream(),
        PolicyExportResult.class);
    String suffix = TemporaryEntity.uuid();
    appName = APPLICATION_NAME_PREFIX + "-" + suffix;
    Organization org = tempEntity.newOrganization(ORGANIZATION_NAME_PREFIX + "-" + suffix);
    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);
    app = tempEntity.newApplication(appName, appName, org.getId());
  }

  private void evaluateCannedReport() throws IOException {
    URL zippedReport = ReportHelper.zipReport(REPORT_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work)
        .evaluatePolicy();
  }

  private void openReportAndLogin() {
    playwrightRefreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    playwrightLogin();
    new ApplicationReportPageAssertions(new ApplicationReportPage()).shouldShowReportHeaderContaining(appName);
  }

  @Test
  @Category(SanityTest.class)
  public void testSummaryIndicators() {
    ApplicationReportPage report = new ApplicationReportPage();
    ApplicationReportPageAssertions reportAssert = new ApplicationReportPageAssertions(report);

    reportAssert.shouldShowReportHeaderContaining("Build Report");

    assertThat(report.threatIndicatorsCritical()).containsText(EXPECTED_THREAT_CRITICAL);
    assertThat(report.threatIndicatorsSevere()).containsText(EXPECTED_THREAT_SEVERE);
    assertThat(report.threatIndicatorsModerate()).containsText(EXPECTED_THREAT_MODERATE);
    assertThat(report.threatIndicatorsCaption()).containsText(EXPECTED_VIOLATIONS_CAPTION);
    assertThat(report.threatIndicatorsSubCaption()).containsText(EXPECTED_VIOLATIONS_SUB_CAPTION);

    assertThat(report.coverageCaption()).containsText(EXPECTED_COVERAGE_CAPTION);
    assertThat(report.coverageSubCaption()).containsText(EXPECTED_COVERAGE_SUB_CAPTION);
  }

  @Test
  @Category(SanityTest.class)
  public void testAggregateByComponentToggle() {
    ApplicationReportPage report = new ApplicationReportPage();
    new ApplicationReportPageAssertions(report).shouldBeVisible();

    assertThat(report.violationRows()).hasCount(EXPECTED_VIOLATION_ROW_COUNT);

    report.componentFilter().fill(COMPONENT_FILTER_TERM);
    assertThat(report.violationRows()).hasCount(EXPECTED_FILTERED_VIOLATION_ROW_COUNT);

    report.aggregateByComponentToggle().click();
    assertThat(report.violationRows()).hasCount(EXPECTED_FILTERED_TOTAL_ROW_COUNT);

    report.componentFilter().fill("");
    assertThat(report.violationRows()).hasCount(EXPECTED_TOTAL_ROW_COUNT);
  }

  @Test
  @Category(SanityTest.class)
  public void testReevaluate() throws IOException {
    Policy licenseBanned = lookup(PolicyDAO.class).getByName("License-Banned").get(0);
    tempEntity.newWaiver(licenseBanned.getId(), app.getId());
    stubReevaluationEndpoint();

    playwrightRefreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

    ApplicationReportPage report = new ApplicationReportPage();
    new ApplicationReportPageAssertions(report).shouldBeVisible();

    report.componentFilter().fill("mycila");
    assertThat(report.violationRows()).hasCount(1);
    assertThat(report.violationRows().first().locator(".iq-waiver-indicator")).isHidden();

    report.triggerFullReevaluationAndWait();

    assertThat(report.componentFilter()).hasValue("mycila");
    assertThat(report.violationRows()).hasCount(1);
    assertThat(report.violationRows().first().locator(".iq-waiver-indicator")).isVisible();
  }

  @Test
  @Category(SanityTest.class)
  public void testBackNavigation() {
    ApplicationReportPage report = new ApplicationReportPage();
    new ApplicationReportPageAssertions(report).shouldBeVisible();

    report.backButton().click();
    playwrightWaitUntilUrlContains("/reports/violations");
    assertThat(new ReportListPage().container()).isVisible();
  }

  private void stubReevaluationEndpoint() throws IOException {
    URL zippedReport = ReportHelper.zipReport(REPORT_DIR, tempDir);
    testCLMServer.getHdsServer()
        .respondWith(zippedReport)
        .atUri("rest/application/analysis/" + HdsMockServer.RestServlet.SCAN_ID);
  }
}
