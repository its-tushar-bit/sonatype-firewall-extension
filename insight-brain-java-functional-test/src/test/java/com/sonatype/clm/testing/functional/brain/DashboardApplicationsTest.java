/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationTile;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationsHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationsResults;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.pages.ApplicationReportContainerPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.proxy.ResponseCopyHandler;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;

import com.codeborne.selenide.ElementsCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.IqConditions.allHaveClass;
import static com.sonatype.clm.testing.functional.utils.IqConditions.cssValues;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DashboardApplicationsTest
    extends AbstractFunctionalTest
{
  private static final String NO_DATA_MSG = "No data available given the applied filters and available permissions.";

  private static final String MAX_RESULTS_MSG = "First 100 results shown";

  private int componentCounter;

  private Organization org;

  private Policy policy;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.APPLICATIONS_URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    componentCounter = 0;
    org = tempEntity.newOrganization("DashboardApplicationsTest");
    policy = tempEntity.newPolicy(org.getId(), "DashboardApplicationsTestPolicy");
    refreshOrOpen(DashboardPage.APPLICATIONS_URL);
  }

  @After
  public void cleanup() {
    clearFilters();
    reverseProxyServer.reset();
  }

  @Test
  public void testResultsMessages() {
    ApplicationsResults table = DashboardPage.applicationsView().results();

    // no results
    refresh();
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    // 100 results
    createApplicationsWithViolation(100);
    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldNotBe(visible);
    table.applications().shouldHaveSize(100);

    // 101 results
    createViolation(createApp(101), BuildStageType.ID, 5);
    refresh();
    table.maxResultsMessage().shouldBe(visible).shouldHave(text(MAX_RESULTS_MSG));
  }

  @Test
  public void testApplicationsTable() {

    // create an app per Threat level and build stage
    createViolation(createApp(1), BuildStageType.ID, 1);
    createViolation(createApp(2), ReleaseStageType.ID, 3);
    createViolation(createApp(3), OperateStageType.ID, 7);
    createViolation(createApp(4), StageReleaseStageType.ID, 10);

    // create single app with violations per threat level and build stage
    Application app = createApp(5);
    createViolation(app, BuildStageType.ID, 8);
    createViolation(app, ReleaseStageType.ID, 4);
    createViolation(app, OperateStageType.ID, 2);
    createViolation(app, StageReleaseStageType.ID, 0);

    refresh();
    showLowRiskViolations();
    DashboardPage.dashboardContainer().shouldBe(visible);
    ApplicationsResults table = DashboardPage.applicationsView().results();

    // applications should be sorted by total risk
    table.applications().shouldHaveSize(5).shouldHave(texts(
        "App5", //
        "App4", //
        "App3", //
        "App2", //
        "App1"  //
    ));

    // check rows per application
    table.application(0).getRows().shouldHaveSize(5);
    table.application(1).getRows().shouldHaveSize(2);
    table.application(2).getRows().shouldHaveSize(2);
    table.application(3).getRows().shouldHaveSize(2);
    table.application(4).getRows().shouldHaveSize(2);

    // check app totals and report links
    ApplicationTile app5 = table.application(0);
    ElementsCollection app5Totals = app5.getTotalsInRow(0);
    app5Totals.shouldHave(texts("14", "8", "4", "2", "0"), allHaveClass("heatmap-cell"))
        .shouldHave(cssValues("background-color", "rgba(40, 69, 91, 1)", "rgba(83, 139, 183, 1)", // heatmap
            "rgba(121, 165, 198, 1)", "rgba(190, 212, 228, 1)", "rgba(247, 251, 255, 1)"));
    app5Totals.get(0).shouldHave(cssClass("white-text"));
    app5Totals.get(1).shouldHave(cssClass("white-text"));
    app5Totals.get(2).shouldNotHave(cssClass("white-text"));
    app5Totals.get(3).shouldNotHave(cssClass("white-text"));
    app5Totals.get(4).shouldNotHave(cssClass("white-text"));
    app5.getTotalsInRow(1).shouldHave(texts("8", "8", "0", "0", "0"))
        .shouldHave(cssValues("background-color", "rgba(0, 0, 0, 0)", "rgba(0, 0, 0, 0)", // no heatmap
            "rgba(0, 0, 0, 0)", "rgba(0, 0, 0, 0)", "rgba(0, 0, 0, 0)"));
    app5.getTotalsInRow(2).shouldHave(texts("0", "0", "0", "0", "0"));
    app5.getTotalsInRow(3).shouldHave(texts("4", "0", "4", "0", "0"));
    app5.getTotalsInRow(4).shouldHave(texts("2", "0", "0", "2", "0"));
    app5.getStageLinks().shouldHaveSize(4).shouldHave(texts(
        "Build",          //
        "Stage Release",  //
        "Release",        //
        "Operate"         //
    ));
    app5.getStageLink(0).shouldHave(attribute("href", ApplicationReportContainerPage.url("5", "App5build")));
    app5.getStageLink(1).shouldHave(attribute("href", ApplicationReportContainerPage.url("5", "App5stage-release")));
    app5.getStageLink(2).shouldHave(attribute("href", ApplicationReportContainerPage.url("5", "App5release")));
    app5.getStageLink(3).shouldHave(attribute("href", ApplicationReportContainerPage.url("5", "App5operate")));

    // sort by name
    ApplicationsHeaders headers = DashboardPage.applicationsView().headers();
    headers.applicationNameHeader().click();
    table.applications().shouldHave(texts(
        "App1", //
        "App2", //
        "App3", //
        "App4", //
        "App5"  //
    ));

    // sort by Low Risk
    headers.lowRiskHeader().click();
    table.firstApplication().shouldHave(text("App1"));
    headers.lowRiskHeader().click();
    table.lastApplication().shouldHave(text("App1"));

    // sort by Moderate Risk
    headers.moderateRiskHeader().click();
    table.firstApplication().shouldHave(text("App2"));
    headers.moderateRiskHeader().click();
    table.lastApplication().shouldHave(text("App2"));

    // sort by Severe Risk
    headers.severeRiskHeader().click();
    table.firstApplication().shouldHave(text("App3"));
    headers.severeRiskHeader().click();
    table.lastApplication().shouldHave(text("App3"));

    // sort by Critical Risk
    headers.criticalRiskHeader().click();
    table.firstApplication().shouldHave(text("App4"));
    headers.criticalRiskHeader().click();
    table.lastApplication().shouldHave(text("App4"));

    // CSV export with no filters
    ResponseCopyHandler responseCopyHandler = new ResponseCopyHandler(
        "/rest/dashboard/export/applicationRisks", testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(responseCopyHandler);
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Applications Data")).click();
    DashboardPage.exportResultsLink().shouldNotBe(visible);
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page
    String exportCsv = new String(responseCopyHandler.consumeResponse());
    String[] expectedResults = {
        "App1,1,0,0,0,1",   //
        "App2,3,0,0,3,0",   //
        "App3,7,0,7,0,0",   //
        "App4,10,10,0,0,0", //
        "App5,14,8,4,2,0"   //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // CSV export - filter out threat level 1
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(2, 10);
    DashboardFilters.apply();
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        "App2,3,0,0,3,0",   //
        "App3,7,0,7,0,0",   //
        "App4,10,10,0,0,0", //
        "App5,14,8,4,2,0"   //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // CSV export - filter out Release violations
    DashboardFilters.stageFilter().twisty().click();
    DashboardFilters.stageFilter().allItems().click();
    DashboardFilters.stageFilter().release().click();
    DashboardFilters.apply();
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        "App3,7,0,7,0,0",   //
        "App4,10,10,0,0,0", //
        "App5,10,8,0,2,0"   //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // CSV export - filter out app4
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().allItems().click();
    DashboardFilters.applicationFilter().checkboxItem(5).click();
    DashboardFilters.apply();
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        "App3,7,0,7,0,0",   //
        "App5,10,8,0,2,0"   //
    };
    assertApplicationsCsv(exportCsv, expectedResults);
  }

  private void assertApplicationsCsv(String csv, String[] expectedSortedResults) {
    String[] lines = csv.split("\r\n");
    assertEquals("Application Name,Total Risk,Critical,Severe,Moderate,Low", lines[0]);
    String[] results = Arrays.copyOfRange(lines, 1, lines.length);
    Arrays.sort(results);
    assertArrayEquals(Arrays.toString(results), expectedSortedResults, results);
  }

  private Application createApp(int index) {
    return tempEntity.newApplication("App" + index, "" + index, org.getId());
  }

  private PolicyEvaluation createEvaluation(Application app, String stageType) {
    return tempEntity.newPolicyEvaluation(app.getId(), stageType, app.getName() + stageType);
  }

  private PolicyViolation createViolation(Application app, String stageType, int threatLevel) {
    PolicyEvaluation evaluation = createEvaluation(app, stageType);
    int componentIndex = componentCounter++;
    String group = "Group" + componentIndex;
    String artifact = "Artifact" + componentIndex;
    String version = "Version" + componentIndex;
    String hash = "hash" + componentIndex;

    return tempEntity.newPolicyViolation(evaluation, policy, threatLevel,
        PolicyThreatCategory.LICENSE, group, artifact, version, hash, FailActionType.ID);
  }

  private void createApplicationsWithViolation(int numberOfApps) {
    for (int i = 1; i <= numberOfApps; i++) {
      createViolation(createApp(i), BuildStageType.ID, 5);
    }
  }

  private void showLowRiskViolations() {
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(0, 10);
    DashboardFilters.apply();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
  }

  private void clearFilters() {
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    List<DashboardFilter> filters = dashboardFilterDAO.getByUsername("admin");
    for (DashboardFilter filter : filters) {
      dashboardFilterDAO.delete(filter);
    }
  }
}
