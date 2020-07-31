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
import org.openqa.selenium.Keys;

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
    PolicyImportExport policyImportExport = new PolicyImportExport();

    Organization org = tempEntity.newOrganization("ApplicationReportTestOrgWithAReallyLongName");
    policyImportExport.importOrganization(org, referencePolicies);
    tempEntity.newUser("user1", "reallylongfirst", "even longer last name junior senior", "a@a.com");
    app = tempEntity.newApplication("ApplicationReportTestWithAReallyLongName",
        "ApplicationReportTestWithAReallyLongName", org.getId(), "user1");
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

    refreshOrOpen(ReportListPage.url());
  }

  @Test
  public void testTooltips() {
    ReportListRow firstRow = ReportListPage.firstRow();
    firstRow.shouldBe(visible);

    firstRow.applicationNameTooltip().shouldNot(exist);
    firstRow.applicationName().hover();
    firstRow.applicationNameTooltip().should(exist);
    firstRow.applicationNameTooltip().shouldHave(exactText("ApplicationReportTestWithAReallyLongName"));

    firstRow.contactNameTooltip().shouldNot(exist);
    firstRow.contactName().hover();
    firstRow.contactNameTooltip().should(exist);
    firstRow.contactNameTooltip().shouldHave(exactText("reallylongfirst even longer last name junior senior"));

    firstRow.organizationNameTooltip().shouldNot(exist);
    firstRow.organizationName().hover();
    firstRow.organizationNameTooltip().should(exist);
    firstRow.organizationNameTooltip().shouldHave(exactText("ApplicationReportTestOrgWithAReallyLongName"));
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

  @Test
  public void testSearch() {
    Organization org1 = tempEntity.newOrganization("nameOneOrg");
    Organization org2 = tempEntity.newOrganization("nameTwOOrg");
    Organization org3 = tempEntity.newOrganization("nameThreeOrg");
    Application app1 = tempEntity.newApplication("nameOneApp", "publicId1", org3.getId());
    Application app2 = tempEntity.newApplication("nametwoApp", "publicId2", org1.getId());
    Application app3 = tempEntity.newApplication("nameThreeApp", "publicId3", org2.getId());
    List<Application> apps = new ApplicationDAO().getAll();

    refresh();

    ReportListPage.rows().shouldHaveSize(apps.size()).forEach(selenideElement -> selenideElement.shouldBe(visible));

    ReportListPage.filter().setValue(app1.getName());
    ReportListPage.search().click();
    ReportListPage.rows().shouldHaveSize(1);
    ReportListPage.firstRow().applicationName().shouldHave(text(app1.getName()));
    ReportListPage.firstRow().organizationName().shouldHave(text(org3.getName()));

    ReportListPage.filter().setValue(org2.getName()).sendKeys(Keys.ENTER);
    ReportListPage.rows().shouldHaveSize(1);
    ReportListPage.firstRow().applicationName().shouldHave(text(app3.getName()));
    ReportListPage.firstRow().organizationName().shouldHave(text(org2.getName()));

    ReportListPage.filter().setValue("tWo").sendKeys(Keys.ENTER);
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

  @Test
  public void testOrder() {
    List<Organization> orgs = new ArrayList<>();
    List<Application> apps = new ArrayList<>();
    createAlphabeticalOrgsAndApps(orgs, apps);
    refresh();

    // App name ascending
    ReportListPage.sortAscending(ReportListPage.applicationNameHeader());
    List<String> names = new ArrayList<>();
    ReportListPage.consumeAllRows(row -> names.add(row.applicationName().getText()));
    apps.sort(Comparator.comparing(Application::getName, String.CASE_INSENSITIVE_ORDER));
    assertThat(names).isEqualTo(apps.subList(0, ReportListPage.RESULTS_PER_PAGE).stream().map(Application::getName)
        .collect(Collectors.toList()));

    // App name descending
    ReportListPage.sortDescending(ReportListPage.applicationNameHeader());
    names.clear();
    ReportListPage.consumeAllRows(row -> names.add(row.applicationName().getText()));
    apps.sort(Comparator.comparing(Application::getName, String.CASE_INSENSITIVE_ORDER).reversed());
    assertThat(names).isEqualTo(apps.subList(0, ReportListPage.RESULTS_PER_PAGE).stream().map(Application::getName)
        .collect(Collectors.toList()));

    // Org name ascending
    ReportListPage.sortAscending(ReportListPage.organizationNameHeader());
    names.clear();
    ReportListPage.consumeAllRows(row -> names.add(row.organizationName().getText()));
    orgs.sort(Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER));
    assertThat(names).isEqualTo(orgs.subList(0, ReportListPage.RESULTS_PER_PAGE).stream().map(Organization::getName)
        .collect(Collectors.toList()));

    // Org name descending
    ReportListPage.sortDescending(ReportListPage.organizationNameHeader());
    names.clear();
    ReportListPage.consumeAllRows(row -> names.add(row.organizationName().getText()));
    orgs.sort(Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER).reversed());
    assertThat(names).isEqualTo(orgs.subList(0, ReportListPage.RESULTS_PER_PAGE).stream().map(Organization::getName)
        .collect(Collectors.toList()));
  }

  private void createAlphabeticalOrgsAndApps(List<Organization> orgs, List<Application> apps) {
    orgs.addAll(new OrganizationDAO().getAll().stream()
        .filter(org -> !org.getId().equals(Organization.ROOT_ORGANIZATION_ID)).collect(Collectors.toList()));
    apps.addAll(new ApplicationDAO().getAll());
    int currentSize = apps.size();
    for (int result = 0; result < ReportListPage.RESULTS_PER_PAGE + 1 - currentSize; result++) {
      String orgSuffix = getAlphabeticalSequenceElement(result);
      String appSuffix = getAlphabeticalSequenceElement(result + 1);
      Organization org = tempEntity.newOrganization("orgName" + orgSuffix);
      orgs.add(org);
      apps.add(tempEntity.newApplication("appName" + appSuffix, "appPublicId" + appSuffix, org.getId()));
    }
  }

  private String getAlphabeticalSequenceElement(int i) {
    return i < 0 ? "" : getAlphabeticalSequenceElement((i / 26) - 1) + (char) (65 + i % 26);
  }
}
