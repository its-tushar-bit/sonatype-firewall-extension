/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationTile;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationsHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationsResults;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
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

import com.codeborne.selenide.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;

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
    open(DashboardPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    componentCounter = 0;
    org = tempEntity.newOrganization("DashboardApplicationsTest");
    policy = tempEntity.newPolicy(org.getId(), "DashboardApplicationsTestPolicy");
  }

  @After
  public void cleanup() {
    clearFilters();
  }

  @Test
  public void testResultsMessages() {
    ApplicationsResults table = DashboardPage.applicationsResults();

    // no results
    refreshOrOpen(DashboardPage.APPLICATIONS_URL);
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    // 100 results
    createApplicationsWithViolation(100);
    refreshOrOpen(DashboardPage.APPLICATIONS_URL);
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldNotBe(visible);
    table.applications().shouldHaveSize(100);

    // 101 results
    createViolation(createApp(101), BuildStageType.ID, 5);
    refreshOrOpen(DashboardPage.APPLICATIONS_URL);
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

    refreshOrOpen(DashboardPage.APPLICATIONS_URL);
    showLowRiskViolations();
    DashboardPage.dashboardContainer().shouldBe(visible);
    ApplicationsResults table = DashboardPage.applicationsResults();

    // applications should be sorted by name
    table.applications().shouldHaveSize(5).shouldHave(texts(
        "App1", //
        "App2", //
        "App3", //
        "App4", //
        "App5"  //
    ));

    // check rows per application
    table.application(0).getRows().shouldHaveSize(2);
    table.application(1).getRows().shouldHaveSize(2);
    table.application(2).getRows().shouldHaveSize(2);
    table.application(3).getRows().shouldHaveSize(2);
    table.application(4).getRows().shouldHaveSize(5);

    // check app totals and report links
    ApplicationTile app5 = table.application(4);
    app5.getTotalsInRow(0).shouldHave(texts("14", "8", "4", "2", "0", ""));
    app5.getTotalsInRow(1).shouldHave(texts("8", "8", "0", "0", "0", ""));
    app5.getTotalsInRow(2).shouldHave(texts("0", "0", "0", "0", "0", ""));
    app5.getTotalsInRow(3).shouldHave(texts("4", "0", "4", "0", "0", ""));
    app5.getTotalsInRow(4).shouldHave(texts("2", "0", "0", "2", "0", ""));
    app5.getStageLinks().shouldHaveSize(4).shouldHave(texts(
        "Build",          //
        "Stage Release",  //
        "Release",        //
        "Operate"         //
    ));
    String urlPrefix = Configuration.baseUrl + "assets/index.html#/reports/5/";
    app5.getStageLink(0).shouldHave(attribute("href", urlPrefix + "App5build"));
    app5.getStageLink(1).shouldHave(attribute("href", urlPrefix + "App5stage-release"));
    app5.getStageLink(2).shouldHave(attribute("href", urlPrefix + "App5release"));
    app5.getStageLink(3).shouldHave(attribute("href", urlPrefix + "App5operate"));


    // sort by totalRisk
    ApplicationsHeaders headers = DashboardPage.applicationsHeaders();
    headers.totalRiskHeader().click();
    table.applications().shouldHave(texts(
        "App5", //
        "App4", //
        "App3", //
        "App2", //
        "App1"  //
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
  }

  private Application createApp(int index) {
    return tempEntity.newApplication("App" + index, "" + index, org.getId());
  }

  private PolicyEvaluation createEvaluation(Application app, String stageType) {
    return tempEntity.newPolicyEvaluation(app.getId(), stageType, app.getName() + stageType, new Date());
  }

  private PolicyViolation createViolation(PolicyEvaluation evaluation, int threatLevel) {
    int componentIndex = componentCounter++;
    String group = "Group" + componentIndex;
    String artifact = "Artifact" + componentIndex;
    String version = "Version" + componentIndex;
    String hash = "hash" + componentIndex;

    return tempEntity.newPolicyViolation(evaluation, policy, threatLevel,
        PolicyThreatCategory.LICENSE, group, artifact, version, hash, FailActionType.ID);
  }

  private PolicyViolation createViolation(Application app, String stageType, int threatLevel) {
    return createViolation(createEvaluation(app, stageType), threatLevel);
  }

  private void createApplicationsWithViolation(int numberOfApps) {
    for (int i = 1; i <= numberOfApps; i++) {
      createViolation(createApp(i), BuildStageType.ID, 5);
    }
  }

  private void showLowRiskViolations() {
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(0, 10);
    DashboardFilters.applyButton().click();
  }

  private void clearFilters() {
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    DashboardFilter filter = dashboardFilterDAO.getByUsername("admin");
    dashboardFilterDAO.delete(filter);
  }
}
