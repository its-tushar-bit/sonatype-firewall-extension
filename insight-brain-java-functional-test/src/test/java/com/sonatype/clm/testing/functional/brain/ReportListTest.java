/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQThreatIndicators;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage.ReportListRow;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class ReportListTest
    extends AbstractFunctionalTest
{
  public static final String BUILD_SCAN_ID = "BUILD_SCAN_ID";

  public static final String STAGE_SCAN_ID = "STAGE_SCAN_ID";

  private static final String CANNED_LARGE_REPORT_URI = "/canned-reports/large-report";

  private static final String CANNED_SMALL_REPORT_URI = "/canned-reports/small-report";

  private SourceControlEventDAO sourceControlEventDAO;

  private OrganizationDAO organizationDAO;

  private ApplicationDAO applicationDAO;

  public Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);

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
  public void testTooltips() {
    ReportListRow firstRow = ReportListPage.firstRow();
    firstRow.shouldBe(visible);

    firstRow.applicationNameTooltip().shouldNot(exist);
    firstRow.applicationName().hover();
    firstRow.applicationNameTooltip().should(exist);
    firstRow.applicationNameTooltip().shouldHave(exactText("ApplicationReportTestWithAReallyLongName"));

    firstRow.organizationName().hover();
    firstRow.organizationNameTooltip().should(exist);
    firstRow.organizationNameTooltip().shouldHave(exactText("ApplicationReportTestOrgWithAReallyLongName"));
  }

  @Test
  public void testContactName() {
    ReportListRow firstRow = ReportListPage.firstRow();
    firstRow.shouldBe(visible);

    firstRow.showContactName().shouldBe(visible);
    firstRow.showContactName().click();
    firstRow.contactName().should(visible);
    firstRow.contactName().shouldHave(exactText("reallylongfirst even longer last name junior senior"));

    firstRow.contactName().hover();
    firstRow.contactNameTooltip().should(visible);
    firstRow.contactNameTooltip().shouldHave(exactText("reallylongfirst even longer last name junior senior"));
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
  public void testSourceStage() throws Exception {
    // given: initial checks for app with no source control scans
    final String pendingExpectedText = "pending";
    ReportListRow firstRow = ReportListPage.firstRow();
    firstRow.shouldBe(visible);

    SelenideElement sourceStageCell = firstRow.sourceStageCell();
    sourceStageCell.shouldNot(Condition.text("pending"));
    SelenideElement sourceLink = firstRow.sourceReportLink();
    sourceLink.shouldNotBe(visible);

    // when: request a source control scan
    SourceControlEvent sourceControlEvent = tempEntity.newSourceControlEvaluationEvent(app);
    Selenide.sleep(2000);
    refreshOrOpen(ReportListPage.url());

    // then: source stage should reflect pending scan
    sourceStageCell.should(Condition.text(pendingExpectedText));

    eyesWatcher.eyesCheck("Reports list pending report");

    // when: complete source stage policy eval
    sourceControlEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.update(sourceControlEvent);
    evaluatePolicy("id-source-scan", CANNED_SMALL_REPORT_URI, Stage.ID_SOURCE);
    Selenide.sleep(2000);
    refreshOrOpen(ReportListPage.url());

    // then: pending has gone away and we now have a report
    sourceStageCell.shouldNotHave(Condition.text(pendingExpectedText));
    sourceLink.shouldBe(visible);

    // and then: we can access the report
    ApplicationReportPage reportPage = new ApplicationReportPage();

    sourceLink.click();
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
  public void testSearch() {
    Organization org1 = tempEntity.newOrganization("nameOneOrg");
    Organization org2 = tempEntity.newOrganization("nameTwOOrg");
    Organization org3 = tempEntity.newOrganization("nameThreeOrg");
    Application app1 = tempEntity.newApplication("nameOneApp", "publicId1", org3.getId());
    Application app2 = tempEntity.newApplication("nametwoApp", "publicId2", org1.getId());
    Application app3 = tempEntity.newApplication("nameThreeApp", "publicId3", org2.getId());
    List<Application> apps = applicationDAO.getAll();

    refresh();

    ReportListPage.rows().shouldHave(size(apps.size()));
    ReportListPage.rows().forEach(selenideElement -> selenideElement.shouldBe(visible));

    ReportListPage.filter().setValue(app1.getName());
    ReportListPage.rows().shouldHave(size(1));
    ReportListPage.firstRow().applicationName().shouldHave(text(app1.getName()));
    ReportListPage.firstRow().organizationName().shouldHave(text(org3.getName()));

    ReportListPage.filter().setValue(org2.getName());
    ReportListPage.rows().shouldHave(size(1));
    ReportListPage.firstRow().applicationName().shouldHave(text(app3.getName()));
    ReportListPage.firstRow().organizationName().shouldHave(text(org2.getName()));

    ReportListPage.filter().setValue("tWo");
    ReportListPage.firstRow().applicationName().shouldHave(text(app3.getName()));
    ReportListPage.firstRow().organizationName().shouldHave(text(org2.getName()));
    ReportListPage.row(2).applicationName().shouldHave(text(app2.getName()));
    ReportListPage.row(2).organizationName().shouldHave(text(org1.getName()));
  }

  @Test
  public void testLoad() {
    List<Application> apps = new ArrayList<>();
    createAlphabeticalOrgsAndApps(new ArrayList<>(), apps);
    apps.sort(Comparator.comparing(Application::getName, String.CASE_INSENSITIVE_ORDER));
    ReportListPage.load().shouldNotBe(visible);
    refresh();

    List<String> names = new ArrayList<>();
    ReportListPage.consumeAllRows(row -> names.add(row.applicationName().getText()));
    assertThat(names).isEqualTo(apps.subList(0, ReportListPage.RESULTS_PER_PAGE).stream().map(Application::getName)
        .collect(Collectors.toList()));

    ScrollUtil.awaitEndOfScrolling(ReportListPage.load().scrollIntoView(false).shouldBe(visible));
    assertThat(ReportListPage.row(ReportListPage.rows().size()).applicationName().getText())
        .isNotEqualTo(apps.get(apps.size() - 1).getName());

    ReportListPage.load().click();

    ReportListPage.scrollToBottom();
    ReportListPage.load().shouldNotBe(visible);
    assertThat(ReportListPage.row(ReportListPage.rows().size()).applicationName().getText())
        .isEqualTo(apps.get(apps.size() - 1).getName());
  }

  private void createAlphabeticalOrgsAndApps(List<Organization> orgs, List<Application> apps) {
    orgs.addAll(organizationDAO.getAll().stream()
        .filter(org -> !org.getId().equals(Organization.ROOT_ORGANIZATION_ID)).collect(Collectors.toList()));
    apps.addAll(applicationDAO.getAll());
    for (int result = 0; result < ReportListPage.RESULTS_PER_PAGE; result++) {
      String orgSuffix = getAlphabeticalSequenceElement(result);
      String appSuffix = getAlphabeticalSequenceElement(result);
      Organization org = tempEntity.newOrganization("orgName" + orgSuffix);
      orgs.add(org);
      apps.add(tempEntity.newApplication("appName" + appSuffix, "appPublicId" + appSuffix, org.getId()));
    }
  }

  private String getAlphabeticalSequenceElement(int i) {
    return i < 0 ? "" : getAlphabeticalSequenceElement((i / 26) - 1) + (char) (65 + i % 26);
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
