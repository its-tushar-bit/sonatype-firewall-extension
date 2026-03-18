/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Duration;
import java.util.Arrays;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationElement;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationsHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationsResults;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationsResultsPaginator;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.proxy.ResponseCopyHandler;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.InternalRealm;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.clickable;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.sonatype.clm.testing.functional.utils.IqConditions.allHaveClass;
import static com.sonatype.clm.testing.functional.utils.IqConditions.cssValues;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardApplicationsTest
    extends AbstractFunctionalTest
{
  private static final String NO_DATA_MSG = "No data available given the applied filters and permissions.";

  ApplicationsResults table = DashboardPage.applicationsView().results();

  ApplicationsHeaders headers = DashboardPage.applicationsView().headers();

  private int componentCounter;

  private Organization org;

  private Policy policy;

  private DashboardFilterDAO dashboardFilterDAO;

  private static Dimension originalSize;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.urlToApplications());
    loginAsAdmin();
    originalSize = WebDriverRunner.getWebDriver().manage().window().getSize();
  }

  @Before
  public void init() {
    dashboardFilterDAO = lookup(DashboardFilterDAO.class);

    componentCounter = 0;
    org = tempEntity.newOrganization("DashboardApplicationsTest");
    policy = tempEntity.newPolicy(org);
    setViewportSize(WebDriverRunner.getWebDriver());
    refreshOrOpen(DashboardPage.urlToApplications());
  }

  @After
  public void cleanup() {
    clearFilters();
    reverseProxyServer.reset();
    WebDriverRunner.getWebDriver().manage().window().setSize(originalSize);
  }

  @Test
  public void testResultsMessageNoData() {
    ApplicationsResults table = DashboardPage.applicationsView().results();

    refresh();
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));
  }

  @Test
  public void testApplicationsTable() {

    // create an app per Threat level and build stage
    createViolation(createApp("1"), BuildStageType.ID, 1);
    createViolation(createApp("2"), ReleaseStageType.ID, 3);
    createViolation(createApp("3"), OperateStageType.ID, 7);
    createViolation(createApp("4"), StageReleaseStageType.ID, 10);

    // create single app with violations per threat level and build stage
    Application app = createApp("5");
    createViolation(app, BuildStageType.ID, 8);
    createViolation(app, ReleaseStageType.ID, 4);
    createViolation(app, OperateStageType.ID, 2);
    createViolation(app, StageReleaseStageType.ID, 0);

    refresh();
    showLowRiskViolations();
    DashboardPage.dashboardContainer().shouldBe(visible);

    // applications should be sorted by total risk
    table.applications().shouldHave(size(5));
    table.applications()
        .shouldHave(texts(
            "App5", //
            "App4", //
            "App3", //
            "App2", //
            "App1" //
        ));

    // check rows per application
    table.application(0).getRows().shouldHave(size(5));
    table.application(1).getRows().shouldHave(size(2));
    table.application(2).getRows().shouldHave(size(2));
    table.application(3).getRows().shouldHave(size(2));
    table.application(4).getRows().shouldHave(size(2));

    // check app totals and report links
    ApplicationElement app5 = table.application(0);
    ElementsCollection app5Totals = app5.getTotalsInRow(0);
    app5Totals.shouldHave(texts("14", "8", "4", "2", "0"), allHaveClass("iq-cell--heatmap"))
        .shouldHave(cssValues("background-color", "rgba(40, 69, 91, 1)", "rgba(83, 139, 183, 1)", // heatmap
            "rgba(157, 189, 214, 1)", "rgba(222, 233, 242, 1)", "rgba(247, 251, 255, 1)"));
    app5Totals.get(0).shouldHave(cssClass("white-text"));
    app5Totals.get(1).shouldHave(cssClass("white-text"));
    app5Totals.get(2).shouldNotHave(cssClass("white-text"));
    app5Totals.get(3).shouldNotHave(cssClass("white-text"));
    app5Totals.get(4).shouldNotHave(cssClass("white-text"));
    app5.getTotalsInStageRow(0)
        .shouldHave(texts("8", "8", "0", "0", "0"))
        .shouldHave(cssValues("background-color", "rgba(0, 0, 0, 0)", "rgba(0, 0, 0, 0)", // no heatmap
            "rgba(0, 0, 0, 0)", "rgba(0, 0, 0, 0)", "rgba(0, 0, 0, 0)"));
    app5.getTotalsInStageRow(1).shouldHave(texts("0", "0", "0", "0", "0"));
    app5.getTotalsInStageRow(2).shouldHave(texts("4", "0", "4", "0", "0"));
    app5.getTotalsInStageRow(3).shouldHave(texts("2", "0", "0", "2", "0"));
    app5.getStages().shouldHave(size(4));
    app5.getStages()
        .shouldHave(texts(
            "Build", //
            "Stage Release", //
            "Release", //
            "Operate" //
        ));
    app5.getStageLink(0).shouldHave(attribute("href", ApplicationReportPage.url(app, "App5build")));
    app5.getStageLink(1).shouldHave(attribute("href", ApplicationReportPage.url(app, "App5stage-release")));
    app5.getStageLink(2).shouldHave(attribute("href", ApplicationReportPage.url(app, "App5release")));
    app5.getStageLink(3).shouldHave(attribute("href", ApplicationReportPage.url(app, "App5operate")));

    // check the csv export default sort order
    ResponseCopyHandler responseCopyHandler = new ResponseCopyHandler(
        "/rest/dashboard/export/applicationRisks", testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(responseCopyHandler);
    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Applications Data")).click();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page
    String exportCsv = new String(responseCopyHandler.consumeResponse());
    String[] expectedResults = {
      "DashboardApplicationsTest,App5,14,8,4,2,0", //
      "DashboardApplicationsTest,App4,10,10,0,0,0", //
      "DashboardApplicationsTest,App3,7,0,7,0,0", //
      "DashboardApplicationsTest,App2,3,0,0,3,0", //
      "DashboardApplicationsTest,App1,1,0,0,0,1" //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // sort by name
    headers.applicationNameHeader().click();
    table.applications()
        .shouldHave(texts(
            "App1", //
            "App2", //
            "App3", //
            "App4", //
            "App5" //
        ));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "DashboardApplicationsTest,App1,1,0,0,0,1", //
      "DashboardApplicationsTest,App2,3,0,0,3,0", //
      "DashboardApplicationsTest,App3,7,0,7,0,0", //
      "DashboardApplicationsTest,App4,10,10,0,0,0", //
      "DashboardApplicationsTest,App5,14,8,4,2,0" //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // sort by Low Risk
    headers.lowRiskHeader().click();
    table.firstApplication().shouldHave(text("App1"));
    headers.lowRiskHeader().click();
    table.lastApplication().shouldHave(text("App1"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "DashboardApplicationsTest,App2,3,0,0,3,0", //
      "DashboardApplicationsTest,App3,7,0,7,0,0", //
      "DashboardApplicationsTest,App4,10,10,0,0,0", //
      "DashboardApplicationsTest,App5,14,8,4,2,0", //
      "DashboardApplicationsTest,App1,1,0,0,0,1" //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // sort by Moderate Risk
    headers.moderateRiskHeader().click();
    table.firstApplication().shouldHave(text("App2"));
    headers.moderateRiskHeader().click();
    table.lastApplication().shouldHave(text("App2"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "DashboardApplicationsTest,App1,1,0,0,0,1", //
      "DashboardApplicationsTest,App3,7,0,7,0,0", //
      "DashboardApplicationsTest,App4,10,10,0,0,0", //
      "DashboardApplicationsTest,App5,14,8,4,2,0", //
      "DashboardApplicationsTest,App2,3,0,0,3,0" //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // sort by Severe Risk
    headers.severeRiskHeader().click();
    table.firstApplication().shouldHave(text("App3"));
    headers.severeRiskHeader().click();
    table.lastApplication().shouldHave(text("App3"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "DashboardApplicationsTest,App1,1,0,0,0,1", //
      "DashboardApplicationsTest,App2,3,0,0,3,0", //
      "DashboardApplicationsTest,App4,10,10,0,0,0", //
      "DashboardApplicationsTest,App5,14,8,4,2,0", //
      "DashboardApplicationsTest,App3,7,0,7,0,0" //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // sort by Critical Risk
    headers.criticalRiskHeader().click();
    table.firstApplication().shouldHave(text("App4"));
    headers.criticalRiskHeader().click();
    table.lastApplication().shouldHave(text("App4"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "DashboardApplicationsTest,App1,1,0,0,0,1", //
      "DashboardApplicationsTest,App2,3,0,0,3,0", //
      "DashboardApplicationsTest,App3,7,0,7,0,0", //
      "DashboardApplicationsTest,App5,14,8,4,2,0", //
      "DashboardApplicationsTest,App4,10,10,0,0,0" //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    setViewportSize(WebDriverRunner.getWebDriver());

    // CSV export - filter out threat level 1
    DashboardPage.expandFilter();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(2, 10);
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    eyesWatcher.eyesCheck("Applications tab with form-mask");
    DashboardFilters.apply();

    DashboardFilters.closeButton().shouldBe(clickable, Duration.ofSeconds(5));
    DashboardFilters.closeFilter();
    DashboardFilters.filterContainer().shouldNotBe(visible, Duration.ofSeconds(5));

    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "DashboardApplicationsTest,App2,3,0,0,3,0", //
      "DashboardApplicationsTest,App3,7,0,7,0,0", //
      "DashboardApplicationsTest,App5,14,8,4,2,0", //
      "DashboardApplicationsTest,App4,10,10,0,0,0" //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // CSV export - filter out Release violations
    DashboardPage.expandFilter();
    DashboardFilters.stageFilter().twisty().click();
    DashboardFilters.stageFilter().allItems().click();
    DashboardFilters.stageFilter().release().click();
    DashboardFilters.apply();
    DashboardFilters.closeFilter();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "DashboardApplicationsTest,App3,7,0,7,0,0", //
      "DashboardApplicationsTest,App5,10,8,0,2,0", //
      "DashboardApplicationsTest,App4,10,10,0,0,0" //
    };
    assertApplicationsCsv(exportCsv, expectedResults);

    // CSV export - filter out app4
    DashboardPage.expandFilter();
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().allItems().click();
    DashboardFilters.applicationFilter().checkboxItem(5).click();
    DashboardFilters.apply();
    DashboardFilters.closeFilter();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "DashboardApplicationsTest,App3,7,0,7,0,0", //
      "DashboardApplicationsTest,App5,10,8,0,2,0" //
    };
    assertApplicationsCsv(exportCsv, expectedResults);
  }

  @Test
  public void testApplicationNameTooltip() {
    String appName = "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW";

    createViolation(tempEntity.newApplication(appName, "long", org.getId()),
        BuildStageType.ID, 8, "scan123");
    createViolation(tempEntity.newApplication("A", "short", org.getId()), BuildStageType.ID, 5, "scan123");
    refresh();

    Tooltip.get().shouldBe(hidden);
    table.firstApplication().name().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text(appName));
    table.lastApplication().name().hover();
    Tooltip.get().shouldBe(hidden);
  }

  @Test
  public void testSortsOnFrontend() {
    showLowRiskViolations();

    createViolation(tempEntity.newApplication("Sandbox", "3", org.getId()), BuildStageType.ID, 2);
    createViolation(tempEntity.newApplication("app1", "1", org.getId()), BuildStageType.ID, 4);
    createViolation(tempEntity.newApplication("app2", "2", org.getId()), BuildStageType.ID, 8);

    refresh();
    newFluentWait();

    DashboardPage.dashboardContainer().shouldBe(visible);

    assertThat(table.applications().size()).isEqualTo(3);

    // default - sorted by total risk desc
    headers.totalRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().totalRisk().shouldHave(text("8"));
    table.application(1).totalRisk().shouldHave(text("4"));
    table.lastApplication().totalRisk().shouldHave(text("2"));

    // sort by total risk asc
    headers.totalRiskHeader().click();
    headers.totalRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().totalRisk().shouldHave(text("2"));
    table.application(1).totalRisk().shouldHave(text("4"));
    table.lastApplication().totalRisk().shouldHave(text("8"));

    // sort by name asc and case-insensitive
    headers.applicationNameHeader().click();
    headers.applicationNameHeader().sortArrows().shouldBeUp();
    table.firstApplication().name().shouldHave(text("app1"));
    table.application(1).name().shouldHave(text("app2"));
    table.lastApplication().name().shouldHave(text("Sandbox"));

    // sort by name desc and case-insensitive
    headers.applicationNameHeader().click();
    headers.applicationNameHeader().sortArrows().shouldBeDown();
    table.firstApplication().name().shouldHave(text("Sandbox"));
    table.application(1).name().shouldHave(text("app2"));
    table.lastApplication().name().shouldHave(text("app1"));

    // sort by criticalRisk desc
    headers.criticalRiskHeader().click();
    headers.criticalRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().criticalRisk().shouldHave(text("8"));
    table.application(1).criticalRisk().shouldHave(text("0"));
    table.lastApplication().criticalRisk().shouldHave(text("0"));

    // sort by criticalRisk asc
    headers.criticalRiskHeader().click();
    headers.criticalRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().criticalRisk().shouldHave(text("0"));
    table.application(1).criticalRisk().shouldHave(text("0"));
    table.lastApplication().criticalRisk().shouldHave(text("8"));

    // sort by severeRisk desc
    headers.severeRiskHeader().click();
    headers.severeRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().severeRisk().shouldHave(text("4"));
    table.application(1).severeRisk().shouldHave(text("0"));
    table.lastApplication().severeRisk().shouldHave(text("0"));

    // sort by severeRisk asc
    headers.severeRiskHeader().click();
    headers.severeRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().severeRisk().shouldHave(text("0"));
    table.application(1).severeRisk().shouldHave(text("0"));
    table.lastApplication().severeRisk().shouldHave(text("4"));

    // sort by moderateRisk desc
    headers.moderateRiskHeader().click();
    headers.moderateRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().moderateRisk().shouldHave(text("2"));
    table.application(1).moderateRisk().shouldHave(text("0"));
    table.lastApplication().moderateRisk().shouldHave(text("0"));

    // sort by moderateRisk asc
    headers.moderateRiskHeader().click();
    headers.moderateRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().moderateRisk().shouldHave(text("0"));
    table.application(1).moderateRisk().shouldHave(text("0"));
    table.lastApplication().moderateRisk().shouldHave(text("2"));

    // sort by lowRisk desc
    headers.lowRiskHeader().click();
    headers.lowRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().lowRisk().shouldHave(text("0"));
    table.application(1).lowRisk().shouldHave(text("0"));
    table.lastApplication().lowRisk().shouldHave(text("0"));

    // sort by lowRisk asc
    headers.lowRiskHeader().click();
    headers.lowRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().lowRisk().shouldHave(text("0"));
    table.application(1).lowRisk().shouldHave(text("0"));
    table.lastApplication().lowRisk().shouldHave(text("0"));
  }

  @Test
  public void testSortsOnBackend() {
    showLowRiskViolations();
    createApplicationsWithViolation(40, "low", 1);
    createApplicationsWithViolation(40, "moderate", 2);
    createApplicationsWithViolation(40, "severe", 4);
    createApplicationsWithViolation(40, "critical", 8);
    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);

    // default - sorted by total risk desc
    headers.totalRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().totalRisk().shouldHave(text("8"));
    table.application(40).totalRisk().shouldHave(text("4"));
    table.application(80).totalRisk().shouldHave(text("2"));
    table.lastApplication().totalRisk().shouldHave(text("2"));

    // sort by total risk asc
    headers.totalRiskHeader().click();
    headers.totalRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().totalRisk().shouldHave(text("1"));
    table.application(40).totalRisk().shouldHave(text("2"));
    table.application(80).totalRisk().shouldHave(text("4"));
    table.lastApplication().totalRisk().shouldHave(text("4"));

    // sort by name asc
    headers.applicationNameHeader().click();
    headers.applicationNameHeader().sortArrows().shouldBeUp();
    table.firstApplication().name().shouldHave(text("critical"));
    table.application(40).name().shouldHave(text("low"));
    table.application(80).name().shouldHave(text("moderate"));
    table.lastApplication().name().shouldHave(text("moderate"));

    // sort by name desc
    headers.applicationNameHeader().click();
    headers.applicationNameHeader().sortArrows().shouldBeDown();
    table.firstApplication().name().shouldHave(text("severe"));
    table.application(40).name().shouldHave(text("moderate"));
    table.application(80).name().shouldHave(text("low"));
    table.lastApplication().name().shouldHave(text("low"));

    // sort by criticalRisk desc
    headers.criticalRiskHeader().click();
    headers.criticalRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().criticalRisk().shouldHave(text("8"));
    table.application(40).criticalRisk().shouldHave(text("0"));
    table.lastApplication().criticalRisk().shouldHave(text("0"));

    // sort by criticalRisk asc
    headers.criticalRiskHeader().click();
    headers.criticalRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().criticalRisk().shouldHave(text("0"));
    table.application(40).criticalRisk().shouldHave(text("0"));
    table.lastApplication().criticalRisk().shouldHave(text("0"));

    // sort by severeRisk desc
    headers.severeRiskHeader().click();
    headers.severeRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().severeRisk().shouldHave(text("4"));
    table.application(40).severeRisk().shouldHave(text("0"));
    table.lastApplication().severeRisk().shouldHave(text("0"));

    // sort by severeRisk asc
    headers.severeRiskHeader().click();
    headers.severeRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().severeRisk().shouldHave(text("0"));
    table.application(40).severeRisk().shouldHave(text("0"));
    table.lastApplication().severeRisk().shouldHave(text("0"));

    // sort by moderateRisk desc
    headers.moderateRiskHeader().click();
    headers.moderateRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().moderateRisk().shouldHave(text("2"));
    table.application(40).moderateRisk().shouldHave(text("0"));
    table.lastApplication().moderateRisk().shouldHave(text("0"));

    // sort by moderateRisk asc
    headers.moderateRiskHeader().click();
    headers.moderateRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().moderateRisk().shouldHave(text("0"));
    table.application(40).moderateRisk().shouldHave(text("0"));
    table.lastApplication().moderateRisk().shouldHave(text("0"));

    // sort by lowRisk desc
    headers.lowRiskHeader().click();
    headers.lowRiskHeader().sortArrows().shouldBeDown();
    table.firstApplication().lowRisk().shouldHave(text("1"));
    table.application(40).lowRisk().shouldHave(text("0"));
    table.lastApplication().lowRisk().shouldHave(text("0"));

    // sort by lowRisk asc
    headers.lowRiskHeader().click();
    headers.lowRiskHeader().sortArrows().shouldBeUp();
    table.firstApplication().lowRisk().shouldHave(text("0"));
    table.application(40).lowRisk().shouldHave(text("0"));
    table.lastApplication().lowRisk().shouldHave(text("0"));

  }

  @Test
  public void testApplicationsTableMultiplePages() {
    for (int i = 0; i <= 49; i++) {
      createViolation(createApp(String.valueOf(i)), BuildStageType.ID, 1);
    }

    for (int i = 50; i <= 99; i++) {
      createViolation(createApp(String.valueOf(i)), BuildStageType.ID, 3);
    }

    for (int i = 100; i <= 124; i++) {
      createViolation(createApp(String.valueOf(i)), BuildStageType.ID, 7);
    }

    for (int i = 125; i <= 150; i++) {
      createViolation(createApp(String.valueOf(i)), BuildStageType.ID, 10);
    }

    refresh();

    // if the filter sidebar is opened before this it will close itself once loading completes
    DashboardPage.applicationsView().results().shouldBe(visible);

    ApplicationsResultsPaginator paginator = DashboardPage.applicationsView().paginator();

    showLowRiskViolations();

    DashboardPage.dashboardContainer().shouldBe(visible);
    paginator.buttonBar().shouldBe(visible);
    paginator.nextPageButton().shouldBe(visible);
    paginator.previousPageButton().shouldBe(hidden);
    table.firstApplication().totalRisk().shouldBe(text("10"));
    table.lastApplication().totalRisk().shouldBe(text("3"));
    eyesWatcher.eyesCheck("Dashboard applications tab with multiple pages");
    // click next page
    paginator.nextPageButton().click();
    newFluentWait();
    table.firstApplication().totalRisk().shouldBe(text("3"));
    table.lastApplication().totalRisk().shouldBe(text("1"));

    // sort by total risk asc
    headers.totalRiskHeader().click();
    // should be at first page after sorting
    table.firstApplication().totalRisk().shouldBe(text("1"));
    paginator.nextPageButton().click();
    newFluentWait();
    paginator.nextPageButton().shouldBe(hidden);
    paginator.previousPageButton().shouldBe(visible);
    table.lastApplication().totalRisk().shouldBe(text("10"));

    // click previous page
    paginator.previousPageButton().click();
    newFluentWait();
    paginator.nextPageButton().shouldBe(visible);
    paginator.previousPageButton().shouldBe(hidden);
    table.firstApplication().totalRisk().shouldBe(text("1"));
    table.lastApplication().totalRisk().shouldBe(text("3"));

    refresh();
    DashboardPage.applicationsView().results().shouldBe(visible);
    DashboardPage.expandFilter();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(3, 7);
    DashboardFilters.apply();
    DashboardFilters.closeFilter();
    // should be at first page after filtering
    paginator.buttonBar().shouldBe(hidden);
    paginator.nextPageButton().shouldBe(hidden);
    paginator.previousPageButton().shouldBe(hidden);
    table.firstApplication().totalRisk().shouldBe(text("7"));
    table.lastApplication().totalRisk().shouldBe(text("3"));
  }

  @Test
  public void testApplicationsTabDoesNotShowReasonsFilter() {
    DashboardPage.expandFilter();
    DashboardFilters.filterContainer().shouldBe(visible);
    DashboardFilters.iqPolicyWaiverReasonFilter().shouldNotBe(visible);
  }

  private void assertApplicationsCsv(String csv, String[] expectedSortedResults) {
    String[] lines = csv.split("\r\n");
    assertThat(lines[0]).isEqualTo("Organization Name,Application Name,Total Risk,Critical,Severe,Moderate,Low");
    String[] results = Arrays.copyOfRange(lines, 1, lines.length);
    assertThat(results).isEqualTo(expectedSortedResults);
  }

  private Application createApp(String id) {
    return tempEntity.newApplication("App" + id, id, org.getId());
  }

  private PolicyViolation createViolation(Application app, String stageType, int threatLevel) {
    return createViolation(app, stageType, threatLevel, app.getName() + stageType);
  }

  private PolicyViolation createViolation(Application app, String stageType, int threatLevel, String scanId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageType, scanId);
    int componentIndex = componentCounter++;
    String group = "Group" + componentIndex;
    String artifact = "Artifact" + componentIndex;
    String version = "Version" + componentIndex;
    String hash = "hash" + componentIndex;

    return tempEntity.newPolicyViolation(evaluation, policy, threatLevel,
        PolicyThreatCategory.LICENSE, group, artifact, version, hash, FailActionType.ID);
  }

  private void createApplicationsWithViolation(int numberOfApps, String namePrefix, int threatLevel) {
    for (int i = 1; i <= numberOfApps; i++) {
      createViolation(createApp(namePrefix + i), BuildStageType.ID, threatLevel);
    }
  }

  private void showLowRiskViolations() {
    DashboardPage.expandFilter();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(0, 10);
    DashboardFilters.apply();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.closeFilter();
  }

  private void clearFilters() {
    dashboardFilterDAO.deleteByUsernameAndRealmId(User.ADMIN_USERNAME, InternalRealm.ID);
  }

  private void newFluentWait() {
    new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class)
        .until(ExpectedConditions.visibilityOf(table.firstApplication().totalRisk()));
  }
}
