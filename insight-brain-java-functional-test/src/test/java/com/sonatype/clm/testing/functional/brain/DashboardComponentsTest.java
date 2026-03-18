/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Duration;
import java.util.Arrays;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardComponents.ComponentResultsPaginator;
import com.sonatype.clm.testing.functional.elements.DashboardComponents.ComponentsHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardComponents.ComponentsResults;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.proxy.ResponseCopyHandler;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.InternalRealm;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.IqConditions.cssValues;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardComponentsTest
    extends AbstractFunctionalTest
{
  private static final String NO_DATA_MSG = "No data available given the applied filters and permissions.";

  private Application app;

  private Policy policy;

  private PolicyEvaluation policyEvaluation;

  private DashboardFilterDAO dashboardFilterDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @After
  public void cleanup() {
    clearFilters();
    reverseProxyServer.reset();
  }

  @Before
  public void init() {
    dashboardFilterDAO = lookup(DashboardFilterDAO.class);

    app = tempEntity.newApplicationWithParent(DashboardComponentsTest.class.getSimpleName());
    policy = tempEntity.newPolicy(app.getParentOwnerId());
    policyEvaluation = tempEntity
        .newPolicyEvaluation(app.getId(), BuildStageType.ID, "DashboardComponentsTestFirstEval");
    refreshOrOpen(DashboardPage.urlToComponents());
  }

  @Test
  public void testMoreThanOnePage() {
    // no results
    refresh();
    ComponentsResults table = DashboardPage.componentsView().results();
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    // 100 results
    addComponents(100, 5);
    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);

    ComponentResultsPaginator paginator = DashboardPage.componentsView().paginator();

    paginator.buttonBar().shouldBe(hidden);
    paginator.nextPageButton().shouldNot(exist);
    paginator.previousPageButton().shouldNot(exist);

    // 101 results
    addComponentWithViolation(101, 5);
    refreshOrOpen(DashboardPage.urlToComponents());
    DashboardPage.dashboardContainer().shouldBe(visible);
    paginator.buttonBar().shouldBe(visible);
    paginator.nextPageButton().shouldBe(visible);
    paginator.previousPageButton().shouldBe(hidden);

    // Click next page
    paginator.nextPageButton().click();
    paginator.nextPageButton().shouldBe(hidden);
    paginator.previousPageButton().shouldBe(visible);

    // Click back page
    paginator.previousPageButton().click();
    paginator.nextPageButton().shouldBe(visible);
    paginator.previousPageButton().shouldBe(hidden);
  }

  @Test
  @Ignore // Flakey see CLM-37534
  public void testComponentsTable() {
    // add a violation for each Risk Level
    addComponentWithViolation(2, 3); // moderate
    addComponentWithViolation(1, 1); // low
    addComponentWithViolation(4, 10); // critical
    addComponentWithViolation(3, 7); // severe
    refreshOrOpen(DashboardPage.urlToComponents());
    waitUntilUrl(DashboardPage.urlToComponents());
    DashboardPage.pageLoadSpinner().shouldNotBe(visible, Duration.ofSeconds(10));
    showLowRiskViolations();

    DashboardPage.dashboardContainer().shouldBe(visible);
    eyesWatcher.eyesCheck();
    ComponentsResults table = DashboardPage.componentsView().results();

    // components should be sorted by risk
    table.components().shouldHave(size(4));
    table.components()
        .shouldHave(texts(
            "Group4 : Artifact4 : Version4", //
            "Group3 : Artifact3 : Version3", //
            "Group2 : Artifact2 : Version2", //
            "Group1 : Artifact1 : Version1" //
        ));

    // test heat map
    table.componentRisks(0)
        .shouldHave(
            cssValues("background-color", "rgba(54, 93, 123, 1)", "rgba(54, 93, 123, 1)", "rgba(247, 251, 255, 1)",
                "rgba(247, 251, 255, 1)", "rgba(247, 251, 255, 1)"));
    // check the text colors
    table.componentRisks(0).get(0).shouldHave(cssClass("white-text"));
    table.componentRisks(0).get(1).shouldHave(cssClass("white-text"));
    table.componentRisks(0).get(2).shouldHave(cssClass("grey-text"));
    table.componentRisks(0).get(3).shouldHave(cssClass("grey-text"));
    table.componentRisks(0).get(4).shouldHave(cssClass("grey-text"));

    table.componentRisks(1)
        .shouldHave(
            cssValues("background-color", "rgba(91, 145, 187, 1)", "rgba(247, 251, 255, 1)", "rgba(91, 145, 187, 1)",
                "rgba(247, 251, 255, 1)", "rgba(247, 251, 255, 1)"));
    table.componentRisks(2)
        .shouldHave(
            cssValues("background-color", "rgba(150, 185, 212, 1)", "rgba(247, 251, 255, 1)", "rgba(247, 251, 255, 1)",
                "rgba(150, 185, 212, 1)", "rgba(247, 251, 255, 1)"));
    table.componentRisks(3)
        .shouldHave(
            cssValues("background-color", "rgba(203, 220, 234, 1)", "rgba(247, 251, 255, 1)", "rgba(247, 251, 255, 1)",
                "rgba(247, 251, 255, 1)", "rgba(203, 220, 234, 1)"));

    // open component details and back
    DashboardComponentDetailsPage dashboardComponentDetailsPage = new DashboardComponentDetailsPage();
    table.firstComponent().name().click();
    DashboardPage.dashboardContainer().shouldBe(hidden);
    dashboardComponentDetailsPage.header().shouldHave(text("Group4 : Artifact4 : Version4"));
    backToViolationsTab();

    table.component(1).name().click();
    DashboardPage.dashboardContainer().shouldBe(hidden);
    dashboardComponentDetailsPage.header().shouldHave(text("Group3 : Artifact3 : Version3"));
    backToViolationsTab();

    table.lastComponent().name().click();
    DashboardPage.dashboardContainer().shouldBe(hidden);
    dashboardComponentDetailsPage.header().shouldHave(text("Group1 : Artifact1 : Version1"));
    backToViolationsTab();

    // check the csv export default sort order
    ResponseCopyHandler responseCopyHandler = new ResponseCopyHandler("/rest/dashboard/export/componentRisks",
        testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(responseCopyHandler);
    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Components Data")).click();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page
    String exportCsv = new String(responseCopyHandler.consumeResponse());
    String[] expectedResults = {
      "Group4 : Artifact4 : Version4,1,10,10,0,0,0",
      "Group3 : Artifact3 : Version3,1,7,0,7,0,0",
      "Group2 : Artifact2 : Version2,1,3,0,0,3,0",
      "Group1 : Artifact1 : Version1,1,1,0,0,0,1"
    };
    assertComponentsCsv(exportCsv, expectedResults);

    ComponentsHeaders headers = DashboardPage.componentsView().headers();

    // sort by Low Risk
    headers.lowRiskHeader().click();
    table.firstComponent().shouldHave(text("Group1 : Artifact1 : Version1"));
    headers.lowRiskHeader().click();
    table.lastComponent().shouldHave(text("Group1 : Artifact1 : Version1"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "Group2 : Artifact2 : Version2,1,3,0,0,3,0",
      "Group3 : Artifact3 : Version3,1,7,0,7,0,0",
      "Group4 : Artifact4 : Version4,1,10,10,0,0,0",
      "Group1 : Artifact1 : Version1,1,1,0,0,0,1"
    };
    assertComponentsCsv(exportCsv, expectedResults);

    // sort by Moderate Risk
    headers.moderateRiskHeader().click();
    table.firstComponent().shouldHave(text("Group2 : Artifact2 : Version2"));
    headers.moderateRiskHeader().click();
    table.lastComponent().shouldHave(text("Group2 : Artifact2 : Version2"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "Group1 : Artifact1 : Version1,1,1,0,0,0,1",
      "Group3 : Artifact3 : Version3,1,7,0,7,0,0",
      "Group4 : Artifact4 : Version4,1,10,10,0,0,0",
      "Group2 : Artifact2 : Version2,1,3,0,0,3,0"
    };
    assertComponentsCsv(exportCsv, expectedResults);

    // sort by Severe Risk
    headers.severeRiskHeader().click();
    table.firstComponent().shouldHave(text("Group3 : Artifact3 : Version3"));
    headers.severeRiskHeader().click();
    table.lastComponent().shouldHave(text("Group3 : Artifact3 : Version3"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "Group1 : Artifact1 : Version1,1,1,0,0,0,1",
      "Group2 : Artifact2 : Version2,1,3,0,0,3,0",
      "Group4 : Artifact4 : Version4,1,10,10,0,0,0",
      "Group3 : Artifact3 : Version3,1,7,0,7,0,0",
    };
    assertComponentsCsv(exportCsv, expectedResults);

    // sort by Critical Risk
    headers.criticalRiskHeader().click();
    table.firstComponent().shouldHave(text("Group4 : Artifact4 : Version4"));
    headers.criticalRiskHeader().click();
    table.lastComponent().shouldHave(text("Group4 : Artifact4 : Version4"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "Group1 : Artifact1 : Version1,1,1,0,0,0,1",
      "Group2 : Artifact2 : Version2,1,3,0,0,3,0",
      "Group3 : Artifact3 : Version3,1,7,0,7,0,0",
      "Group4 : Artifact4 : Version4,1,10,10,0,0,0",
    };
    assertComponentsCsv(exportCsv, expectedResults);

    // CSV export - filter out threat level 1
    DashboardPage.expandFilter();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(2, 10);
    DashboardFilters.apply();
    DashboardFilters.closeFilter();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "Group2 : Artifact2 : Version2,1,3,0,0,3,0",
      "Group3 : Artifact3 : Version3,1,7,0,7,0,0",
      "Group4 : Artifact4 : Version4,1,10,10,0,0,0"
    };
    assertComponentsCsv(exportCsv, expectedResults);

    // CSV export - filter out threat level 3
    DashboardPage.expandFilter();
    DashboardFilters.filterContainer().shouldBe(visible);
    DashboardFilters.policyThreatLevelFilter().slider().setValues(7, 9);
    DashboardFilters.apply();
    DashboardFilters.closeFilter();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
      "Group3 : Artifact3 : Version3,1,7,0,7,0,0"
    };
    assertComponentsCsv(exportCsv, expectedResults);
  }

  @Test
  public void testComponentNameTooltip() {
    addComponentWithViolation("A superficially artificial, perfunctorily slapdash", "protracted and interminable name",
        "to ensure overflow in cell", "hash-b", 7);
    addComponentWithViolation("A", null, null, "hash-a", 3);

    refreshOrOpen(DashboardPage.urlToComponents());
    DashboardPage.dashboardContainer().shouldBe(visible);
    ComponentsResults table = DashboardPage.componentsView().results();

    Tooltip.get().shouldBe(hidden);
    table.firstComponent().name().hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("A superficially artificial, perfunctorily "
            + "slapdash : protracted and interminable name : to ensure overflow in cell"));
    table.lastComponent().name().hover();
    Tooltip.get().shouldBe(hidden);
  }

  @Test
  public void testSortsOnBackend() {
    ComponentsResults table = DashboardPage.componentsView().results();
    ComponentsHeaders headers = DashboardPage.componentsView().headers();

    showLowRiskViolations();
    addComponents(40, 1, "low");
    addComponents(40, 2, "moderate");
    addComponents(40, 4, "severe");
    addComponents(40, 8, "critical");

    // add low risk components also to second app
    app = tempEntity.newApplicationWithParent(DashboardComponentsTest.class.getSimpleName() + 2);
    policy = tempEntity.newPolicy(app.getParentOwnerId());
    policyEvaluation = tempEntity
        .newPolicyEvaluation(app.getId(), BuildStageType.ID, "AnotherAppEval");
    refreshOrOpen(DashboardPage.urlToComponents());
    addComponents(40, 0, "low");

    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);

    // default - sorted by total risk desc
    headers.totalRiskHeader().sortArrows().shouldBeDown();
    table.firstComponent().totalRisk().shouldHave(text("8"));
    table.component(40).totalRisk().shouldHave(text("4"));
    table.component(80).totalRisk().shouldHave(text("2"));
    table.lastComponent().totalRisk().shouldHave(text("2"));

    // sort by total risk asc
    headers.totalRiskHeader().click();
    headers.totalRiskHeader().sortArrows().shouldBeUp();
    table.firstComponent().totalRisk().shouldHave(text("1"));
    table.component(40).totalRisk().shouldHave(text("2"));
    table.component(80).totalRisk().shouldHave(text("4"));
    table.lastComponent().totalRisk().shouldHave(text("4"));

    // sort by number of affected apps desc
    headers.affectedAppsHeader().click();
    headers.affectedAppsHeader().sortArrows().shouldBeDown();
    table.firstComponent().affectedApps().shouldHave(text("2"));
    table.component(40).affectedApps().shouldHave(text("1"));
    table.lastComponent().affectedApps().shouldHave(text("1"));

    // sort by number of affected apps asc
    headers.affectedAppsHeader().click();
    headers.affectedAppsHeader().sortArrows().shouldBeUp();
    table.firstComponent().affectedApps().shouldHave(text("1"));
    table.component(40).affectedApps().shouldHave(text("1"));
    table.lastComponent().affectedApps().shouldHave(text("1"));

    // sort by criticalRisk desc
    headers.criticalRiskHeader().click();
    headers.criticalRiskHeader().sortArrows().shouldBeDown();
    table.firstComponent().criticalRisk().shouldHave(text("8"));
    table.component(40).criticalRisk().shouldHave(text("0"));
    table.lastComponent().criticalRisk().shouldHave(text("0"));

    // sort by criticalRisk asc
    headers.criticalRiskHeader().click();
    headers.criticalRiskHeader().sortArrows().shouldBeUp();
    table.firstComponent().criticalRisk().shouldHave(text("0"));
    table.component(40).criticalRisk().shouldHave(text("0"));
    table.lastComponent().criticalRisk().shouldHave(text("0"));

    // sort by severeRisk desc
    headers.severeRiskHeader().click();
    headers.severeRiskHeader().sortArrows().shouldBeDown();
    table.firstComponent().severeRisk().shouldHave(text("4"));
    table.component(40).severeRisk().shouldHave(text("0"));
    table.lastComponent().severeRisk().shouldHave(text("0"));

    // sort by severeRisk asc
    headers.severeRiskHeader().click();
    headers.severeRiskHeader().sortArrows().shouldBeUp();
    table.firstComponent().severeRisk().shouldHave(text("0"));
    table.component(40).severeRisk().shouldHave(text("0"));
    table.lastComponent().severeRisk().shouldHave(text("0"));

    // sort by moderateRisk desc
    headers.moderateRiskHeader().click();
    headers.moderateRiskHeader().sortArrows().shouldBeDown();
    table.firstComponent().moderateRisk().shouldHave(text("2"));
    table.component(40).moderateRisk().shouldHave(text("0"));
    table.lastComponent().moderateRisk().shouldHave(text("0"));

    // sort by moderateRisk asc
    headers.moderateRiskHeader().click();
    headers.moderateRiskHeader().sortArrows().shouldBeUp();
    table.firstComponent().moderateRisk().shouldHave(text("0"));
    table.component(40).moderateRisk().shouldHave(text("0"));
    table.lastComponent().moderateRisk().shouldHave(text("0"));

    // sort by lowRisk desc
    headers.lowRiskHeader().click();
    headers.lowRiskHeader().sortArrows().shouldBeDown();
    table.firstComponent().lowRisk().shouldHave(text("1"));
    table.component(40).lowRisk().shouldHave(text("0"));
    table.lastComponent().lowRisk().shouldHave(text("0"));

    // sort by lowRisk asc
    headers.lowRiskHeader().click();
    headers.lowRiskHeader().sortArrows().shouldBeUp();
    table.firstComponent().lowRisk().shouldHave(text("0"));
    table.component(40).lowRisk().shouldHave(text("0"));
    table.lastComponent().lowRisk().shouldHave(text("0"));
  }

  @Test
  public void testComponentsTabDoesNotShowReasonsFilter() {
    DashboardPage.expandFilter();
    DashboardFilters.filterContainer().shouldBe(visible);
    DashboardFilters.iqPolicyWaiverReasonFilter().shouldNotBe(visible);
  }

  private void assertComponentsCsv(String csv, String[] expectedSortedResults) {
    String[] lines = csv.split("\r\n");
    assertThat(lines[0]).isEqualTo("Component Name,Affected Apps,Total Risk,Critical,Severe,Moderate,Low");
    String[] results = Arrays.copyOfRange(lines, 1, lines.length);
    assertThat(results).isEqualTo(expectedSortedResults);
  }

  private void addComponents(int numberOfComponents, int riskScore, String suffix) {
    for (int i = 1; i <= numberOfComponents; i++) {
      addComponentWithViolation(i, riskScore, suffix);
    }
  }

  private void addComponents(int numberOfComponents, int riskScore) {
    addComponents(numberOfComponents, riskScore, "");
  }

  private void addComponentWithViolation(int index, int riskScore, String suffix) {
    String group = "Group" + suffix + index;
    String artifact = "Artifact" + suffix + index;
    String version = "Version" + suffix + index;
    String hash = "hash" + suffix + index;
    addComponentWithViolation(group, artifact, version, hash, riskScore);
  }

  private void addComponentWithViolation(int index, int riskScore) {
    addComponentWithViolation(index, riskScore, "");
  }

  private void addComponentWithViolation(String group, String artifact, String version, String hash, int riskScore) {
    tempEntity.newApplicationComponent(app.getId(), policyEvaluation.getStageTypeId(), hash,
        ComponentIdentifier.createMavenCoordinates(group, artifact, version));
    tempEntity.newPolicyViolation(policyEvaluation, policy, riskScore,
        PolicyThreatCategory.LICENSE, group, artifact, version, hash, FailActionType.ID);
  }

  private void showLowRiskViolations() {
    DashboardPage.expandFilter();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(0, 10);
    DashboardFilters.apply();
    DashboardFilters.closeFilter();
    DashboardPage.pageLoadSpinner().shouldNotBe(visible, Duration.ofSeconds(10));
  }

  private void clearFilters() {
    dashboardFilterDAO.deleteByUsernameAndRealmId(User.ADMIN_USERNAME, InternalRealm.ID);
  }

  private void backToViolationsTab() {
    Selenide.back();

    // wait for load spinner to be replaced with contents so we can interact with the page again
    DashboardPage.pageLoadSpinner().shouldNotBe(visible, Duration.ofSeconds(8));
    DashboardPage.dashboardContainer().shouldBe(visible);
  }
}
