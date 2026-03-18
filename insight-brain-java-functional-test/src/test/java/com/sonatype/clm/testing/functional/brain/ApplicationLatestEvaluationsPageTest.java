/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxDropdown;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationLatestEvaluationsPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ReportEntity;
import com.sonatype.insight.brain.report.ReportPurger;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.model.ClientScanType;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.innerText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;

public class ApplicationLatestEvaluationsPageTest
    extends AbstractFunctionalTest
{
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  private final ApplicationLatestEvaluationsPage page = new ApplicationLatestEvaluationsPage();

  private Application application;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent("app");
    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));
  }

  @Test
  public void testApplicationLatestEvaluationsPage() throws Exception {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        false,
        new Date());
    createReport(policyEvaluation);
    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    page.title().shouldBe(visible).shouldHave(exactText(application.getName() + " Latest Evaluations"));
    page.description().shouldBe(visible).shouldHave(exactText("Stage: " + new BuildStageType().getName()));
    page.table().shouldBe(visible);
    ElementsCollection tableHeaders = page.tableHeaders().shouldHave(size(6));
    tableHeaders.get(0).shouldBe(visible).shouldHave(exactText("Evaluation Date"));
    tableHeaders.get(1).shouldBe(visible).shouldHave(exactText("Trigger"));
    tableHeaders.get(2).shouldBe(visible).shouldHave(exactText("Version"));
    tableHeaders.get(3).shouldBe(visible).shouldHave(exactText("Violations"));
    tableHeaders.get(4).shouldBe(visible).shouldHave(exactText("Components"));
    tableHeaders.get(5).shouldBe(visible).shouldBe(empty);
    ElementsCollection tableBodyRowColumns = page.tableBodyRowColumns(0).shouldHave(size(6));
    tableBodyRowColumns.get(0)
        .shouldBe(visible)
        .shouldHave(exactText(DATE_TIME_FORMATTER.format(policyEvaluation.getTime().toInstant())));
    tableBodyRowColumns.get(1)
        .shouldBe(visible)
        .shouldHave(exactText(policyEvaluation.getScanTriggerType().getDisplayName()));
    // Since tempEntity.newPolicyEvaluation sets scanTriggerType to CLI, we only trim the qualifier
    tableBodyRowColumns.get(2).shouldBe(visible).shouldHave(exactText("1.53.0"));
    page.criticalPolicyViolationCount(0).shouldHave(exactText("1"));
    page.severePolicyViolationCount(0).shouldHave(exactText("2"));
    page.moderatePolicyViolationCount(0).shouldHave(exactText("3"));
    tableBodyRowColumns.get(4).shouldBe(visible).shouldHave(exactText("64"));
    tableBodyRowColumns.get(5).shouldBe(visible).shouldHave(exactText("View Report"));
  }

  @Test
  public void testApplicationLatestEvaluationsPage_ScannerVersion_Trimmed() throws Exception {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        false,
        "system",
        ScanTriggerType.WEB_UI,
        ClientScanType.SONATYPE);
    policyEvaluation.setTime(new Date());
    lookup(PolicyEvaluationDAO.class).insert(policyEvaluation);
    createReport(policyEvaluation);
    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    ElementsCollection tableBodyRowColumns = page.tableBodyRowColumns(0).shouldHave(size(6));
    tableBodyRowColumns.get(2).shouldBe(visible).shouldHave(exactText("53"));
  }

  @Test
  public void testApplicationLatestEvaluationsPage_ScannerVersion_DoesNotExist() throws Exception {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        false,
        "system",
        ScanTriggerType.WEB_UI,
        ClientScanType.SONATYPE);
    policyEvaluation.setTime(new Date());
    lookup(PolicyEvaluationDAO.class).insert(policyEvaluation);
    createReport(policyEvaluation);

    // Open the page once so summary.json gets created
    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    // Remove the scannerVersion
    ReportEntity reportEntity = lookup(ApplicationReportPersistenceService.class).getReportEntity(application.getId(),
        policyEvaluation.getScanId(), "summary.json");
    ObjectNode objectNode;
    try (InputStream inputStream = reportEntity.getInputStream()) {
      objectNode = JsonUtils.parse(inputStream.readAllBytes());
    }
    objectNode.remove("scannerVersion");
    try (OutputStream outputStream = reportEntity.getOutputStream()) {
      JsonUtils.write(outputStream, objectNode);
    }

    // Refresh to check how it handles it
    refresh();

    ElementsCollection tableBodyRowColumns = page.tableBodyRowColumns(0).shouldHave(size(6));
    tableBodyRowColumns.get(2).shouldBe(visible).shouldHave(exactText("—"));
  }

  @Test
  public void testApplicationLatestEvaluationsPage_MaxReports() throws Exception {
    Date date = new Date();
    for (int i = 0; i < ApplicationLatestEvaluationsPage.POLICY_EVALUATION_LIMIT + 1; i++) {
      PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
          application.getId(),
          BuildStageType.ID,
          "scan-id-" + i,
          false,
          false,
          DateUtils.addDays(date, -i));
      createReport(policyEvaluation);
    }
    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    page.tableBodyRows().shouldHave(size(ApplicationLatestEvaluationsPage.POLICY_EVALUATION_LIMIT));
  }

  @Test
  public void testApplicationLatestEvaluationsPage_LinksToReport() throws Exception {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        false,
        new Date());
    createReport(policyEvaluation);
    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    page.tableBodyRowColumns(0).get(5).shouldBe(visible).shouldHave(exactText("View Report"));
    page.reportLink(0).click();
    waitUntilUrl(ApplicationReportPage.url(application, policyEvaluation.getScanId()));
  }

  @Test
  public void testApplicationLatestEvaluationsPage_FiltersByStage() throws Exception {
    Date date = new Date();
    PolicyEvaluation buildEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        false,
        DateUtils.addDays(date, -1));
    createReport(buildEvaluation);
    PolicyEvaluation releaseEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        ReleaseStageType.ID,
        "scan-id-1",
        false,
        false,
        date);
    createReport(releaseEvaluation);

    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));
    page.tableBodyRows().shouldHave(size(1));
    page.tableBodyRowColumns(0)
        .get(0)
        .shouldBe(visible)
        .shouldHave(exactText(DATE_TIME_FORMATTER.format(buildEvaluation.getTime().toInstant())));

    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, ReleaseStageType.ID));
    page.tableBodyRows().shouldHave(size(1));
    page.tableBodyRowColumns(0)
        .get(0)
        .shouldBe(visible)
        .shouldHave(exactText(DATE_TIME_FORMATTER.format(releaseEvaluation.getTime().toInstant())));

    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, OperateStageType.ID));
    page.tableBodyRows().shouldHave(size(1));
    page.tableBodyRowColumns(0).shouldHave(size(1)).get(0).shouldBe(visible).shouldHave(exactText("No evaluations"));
  }

  @Test
  public void testApplicationLatestEvaluationsPage_ReevaluationsOneRow() throws Exception {
    Date date = new Date();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        false,
        DateUtils.addDays(date, -1));
    createReport(policyEvaluation);
    PolicyEvaluation policyReEvaluation = tempEntity.newPolicyReEvaluation(
        application.getId(),
        BuildStageType.ID,
        policyEvaluation.getScanId(),
        date);
    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    page.tableBodyRows().shouldHave(size(1));
    ElementsCollection tableBodyRowColumns = page.tableBodyRowColumns(0).shouldHave(size(6));
    tableBodyRowColumns.get(0)
        .shouldBe(visible)
        .shouldHave(exactText(DATE_TIME_FORMATTER.format(policyReEvaluation.getTime().toInstant())));
  }

  @Test
  public void testApplicationLatestEvaluationsPage_ExcludesPurged() throws Exception {
    Date date = new Date();
    for (int i = 0; i < ApplicationLatestEvaluationsPage.POLICY_EVALUATION_LIMIT + 1; i++) {
      PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
          application.getId(),
          BuildStageType.ID,
          "scan-id-" + i,
          false,
          false,
          DateUtils.addDays(date, -i));
      createReport(policyEvaluation);
    }

    DataRetentionPolicy dataRetentionPolicy = new DataRetentionPolicy();
    dataRetentionPolicy.setOwnerId(application.getId());
    dataRetentionPolicy.setContextId(BuildStageType.ID);
    dataRetentionPolicy.setMaxCount(2);
    dataRetentionPolicy.setPurgingEnabled(true);
    lookup(DataRetentionPolicyDAO.class).insert(dataRetentionPolicy);
    lookup(ReportPurger.class).execute(null);

    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    page.tableBodyRows().shouldHave(size(2));
    page.reportLink(0).shouldHave(attribute("href", ApplicationReportPage.url(application, "scan-id-0")));
    page.reportLink(1).shouldHave(attribute("href", ApplicationReportPage.url(application, "scan-id-1")));
  }

  @Test
  public void testApplicationLatestEvaluationsPage_ContinuousMonitoring() throws Exception {
    Date date = new Date();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        false,
        DateUtils.addDays(date, -1));
    createReport(policyEvaluation);
    PolicyEvaluation monitoringEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-1",
        false,
        true,
        date);
    createReport(monitoringEvaluation);

    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    page.tableBodyRows().shouldHave(size(2));
    page.reportLink(0)
        .shouldHave(attribute("href", ApplicationReportPage.url(application, monitoringEvaluation.getScanId())));
    page.tableBodyRowColumns(0)
        .get(1)
        .shouldBe(visible)
        .shouldHave(innerText(policyEvaluation.getScanTriggerType().getDisplayName() + " (Continuous Monitoring)"));
    page.reportLink(1)
        .shouldHave(attribute("href", ApplicationReportPage.url(application, policyEvaluation.getScanId())));
  }

  @Test
  public void testApplicationLatestEvaluationsPage_TruncatedTrigger() throws Exception {
    Date date = new Date();
    PolicyEvaluation policyEvaluationWithLongTriggerName = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        true,
        false,
        date,
        "commitHash0",
        ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING);
    createReport(policyEvaluationWithLongTriggerName);
    PolicyEvaluation policyEvaluationWithShortTriggerName = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-1",
        false,
        false,
        false,
        DateUtils.addDays(date, -1),
        "commitHash1",
        ScanTriggerType.WEB_UI);
    createReport(policyEvaluationWithShortTriggerName);

    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    page.tableBodyRows().shouldHave(size(2));

    SelenideElement longTriggerNameContent = page.tableBodyRowColumns(0).get(1).find(".nx-truncate-ellipsis");
    longTriggerNameContent.shouldBe(visible)
        .shouldHave(attribute("title", "Source Control Default Branch Monitoring"))
        .hover();
    Tooltip.get().shouldBe(visible).shouldHave(exactText("Source Control Default Branch Monitoring"));

    SelenideElement shortTriggerNameContent = page.tableBodyRowColumns(1).get(1).find(".nx-truncate-ellipsis");
    shortTriggerNameContent.shouldBe(visible)
        .shouldHave(attribute("title", ""))
        .hover();
    Tooltip.get().shouldNotBe(visible);
  }

  @Test
  public void testApplicationLatestEvaluationsPage_BackButton_to_ReportsPage() throws Exception {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        false,
        new Date());
    createReport(policyEvaluation);

    refreshOrOpen(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    MainHeader.backButton().should(exactText("All Reports"));
    MainHeader.backButton().shouldBe(visible).click();

    waitUntilUrl(ReportListPage.url());
    ReportListPage.listContainer().shouldBe(visible);
  }

  @Test
  public void testApplicationLatestEvaluationsPage_BackButton_to_ApplicationPage() throws Exception {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "scan-id-0",
        false,
        false,
        new Date());
    createReport(policyEvaluation);

    // we are not using `createReport` because it doesn't produce all the needed files
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator =
        new TestReportEvaluator(application, "scan-id-0", zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(application, "scan-id-0"));

    ApplicationReportPage reportPage = new ApplicationReportPage();
    reportPage.shouldBe(visible);
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();
    optionsDropdown.menu().entries().get(4).shouldHave(text("View Latest Evaluations")).click();
    waitUntilUrl(ApplicationLatestEvaluationsPage.url(application, BuildStageType.ID));

    MainHeader.backButton().should(exactText("Back to Application Report"));
    MainHeader.backButton().shouldBe(visible).click();

    waitUntilUrl(ApplicationReportPage.url(application, policyEvaluation.getScanId()));
    reportPage.reportTitle()
        .shouldBe(visible)
        .shouldHave(exactText(application.getName() + " Build Report"));
  }

  private void createReport(final PolicyEvaluation policyEvaluation) throws Exception {
    Policy policy = tempEntity.newPolicy(application);
    List<PolicyViolation> policyViolations = List.of(
        tempEntity.newPolicyViolation(policyEvaluation, policy, 9, PolicyThreatCategory.SECURITY),
        tempEntity.newPolicyViolation(policyEvaluation, policy, 6, PolicyThreatCategory.SECURITY),
        tempEntity.newPolicyViolation(policyEvaluation, policy, 6, PolicyThreatCategory.SECURITY),
        tempEntity.newPolicyViolation(policyEvaluation, policy, 3, PolicyThreatCategory.SECURITY),
        tempEntity.newPolicyViolation(policyEvaluation, policy, 3, PolicyThreatCategory.SECURITY),
        tempEntity.newPolicyViolation(policyEvaluation, policy, 3, PolicyThreatCategory.SECURITY));
    ReportTestUtils.createReportFile(
        application.getId(),
        policyEvaluation.getScanId(),
        zipReportDir("/canned-reports/large-report", tempDir),
        testCLMServer.getCLMServer().getInstance(InsightWork.class));
    ReportHelper.createPolicyThreats(
        testCLMServer.getCLMServer().getInstance(InsightWork.class),
        application.getId(),
        policyEvaluation.getScanId(),
        policyViolations);
  }
}
