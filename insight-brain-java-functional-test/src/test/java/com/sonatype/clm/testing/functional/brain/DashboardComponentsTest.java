/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardComponentDetails;
import com.sonatype.clm.testing.functional.elements.DashboardComponents.ComponentsHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardComponents.ComponentsResults;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.proxy.ResponseCopyHandler;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.IqConditions.cssValues;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DashboardComponentsTest
    extends AbstractFunctionalTest
{
  private static final String NO_DATA_MSG = "No data available given the applied filters and permissions.";

  private static final String MAX_RESULTS_MSG = "First 100 results shown";

  private Application app;

  private Policy policy;

  private PolicyEvaluation policyEvaluation;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @After
  public void cleanup() {
    clearFilters();
    reverseProxyServer.reset();
  }

  @Before
  public void init() {
    app = tempEntity.newApplicationWithParent(DashboardComponentsTest.class.getSimpleName());
    policy = tempEntity.newPolicy(app.getParentOwnerId(), "DashboardComponentsTestPolicy");
    policyEvaluation = tempEntity
        .newPolicyEvaluation(app.getId(), BuildStageType.ID, "DashboardComponentsTestFirstEval");
    refreshOrOpen(DashboardPage.COMPONENTS_URL);
  }

  @Test
  public void testResultsMessages() {
    // no results
    refresh();
    ComponentsResults table = DashboardPage.componentsView().results();
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    // 100 results
    addComponents(100, 5);
    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldNotBe(visible);

    // 101 results
    addComponentWithViolation(101, 5);
    refreshOrOpen(DashboardPage.COMPONENTS_URL);
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldBe(visible).shouldHave(text(MAX_RESULTS_MSG));
  }

  @Test
  public void testComponentsTable() {
    // add a violation for each Risk Level
    addComponentWithViolation(2, 3);   // moderate
    addComponentWithViolation(1, 1);   // low
    addComponentWithViolation(4, 10);  // critical
    addComponentWithViolation(3, 7);   // severe
    refreshOrOpen(DashboardPage.COMPONENTS_URL);
    showLowRiskViolations();

    DashboardPage.dashboardContainer().shouldBe(visible);
    ComponentsResults table = DashboardPage.componentsView().results();
    table.maxResultsMessage().shouldNotBe(visible);

    // components should be sorted by risk
    table.components().shouldHaveSize(4).shouldHave(texts(
        "Group4 : Artifact4 : Version4",   //
        "Group3 : Artifact3 : Version3",  //
        "Group2 : Artifact2 : Version2",  //
        "Group1 : Artifact1 : Version1"  //
    ));

    // test heat map
    table.componentRisks(0).shouldHave(
        cssValues("background-color", "rgba(54, 93, 123, 1)", "rgba(54, 93, 123, 1)", "rgba(247, 251, 255, 1)",
            "rgba(247, 251, 255, 1)", "rgba(247, 251, 255, 1)"));
    // check the text colors
    table.componentRisks(0).get(0).shouldHave(cssClass("white-text"));
    table.componentRisks(0).get(1).shouldHave(cssClass("white-text"));
    table.componentRisks(0).get(2).shouldHave(cssClass("grey-text"));
    table.componentRisks(0).get(3).shouldHave(cssClass("grey-text"));
    table.componentRisks(0).get(4).shouldHave(cssClass("grey-text"));

    table.componentRisks(1).shouldHave(
        cssValues("background-color", "rgba(91, 145, 187, 1)", "rgba(247, 251, 255, 1)", "rgba(91, 145, 187, 1)",
            "rgba(247, 251, 255, 1)", "rgba(247, 251, 255, 1)"));
    table.componentRisks(2).shouldHave(
        cssValues("background-color", "rgba(150, 185, 212, 1)", "rgba(247, 251, 255, 1)", "rgba(247, 251, 255, 1)",
            "rgba(150, 185, 212, 1)", "rgba(247, 251, 255, 1)"));
    table.componentRisks(3).shouldHave(
        cssValues("background-color", "rgba(203, 220, 234, 1)", "rgba(247, 251, 255, 1)", "rgba(247, 251, 255, 1)",
            "rgba(247, 251, 255, 1)", "rgba(203, 220, 234, 1)"));

    // open component details and back
    DashboardComponentDetails dashboardComponentDetails = new DashboardComponentDetails();
    table.firstComponent().click();
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    dashboardComponentDetails.header().shouldHave(text("Group4 : Artifact4 : Version4"));
    Selenide.navigator.back();
    DashboardPage.dashboardContainer().shouldBe(visible);

    table.component(1).click();
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    dashboardComponentDetails.header().shouldHave(text("Group3 : Artifact3 : Version3"));
    Selenide.navigator.back();
    DashboardPage.dashboardContainer().shouldBe(visible);

    table.lastComponent().click();
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    dashboardComponentDetails.header().shouldHave(text("Group1 : Artifact1 : Version1"));
    Selenide.navigator.back();
    DashboardPage.dashboardContainer().shouldBe(visible);

    // sort by name
    ComponentsHeaders headers = DashboardPage.componentsView().headers();
    headers.componentNameHeader().click();
    table.components().shouldHave(texts(
        "Group1 : Artifact1 : Version1",  //
        "Group2 : Artifact2 : Version2",  //
        "Group3 : Artifact3 : Version3",  //
        "Group4 : Artifact4 : Version4"   //
    ));

    // sort by Low Risk
    headers.lowRiskHeader().click();
    table.firstComponent().shouldHave(text("Group1 : Artifact1 : Version1"));
    headers.lowRiskHeader().click();
    table.lastComponent().shouldHave(text("Group1 : Artifact1 : Version1"));

    // sort by Moderate Risk
    headers.moderateRiskHeader().click();
    table.firstComponent().shouldHave(text("Group2 : Artifact2 : Version2"));
    headers.moderateRiskHeader().click();
    table.lastComponent().shouldHave(text("Group2 : Artifact2 : Version2"));

    // sort by Severe Risk
    headers.severeRiskHeader().click();
    table.firstComponent().shouldHave(text("Group3 : Artifact3 : Version3"));
    headers.severeRiskHeader().click();
    table.lastComponent().shouldHave(text("Group3 : Artifact3 : Version3"));

    // sort by Critical Risk
    headers.criticalRiskHeader().click();
    table.firstComponent().shouldHave(text("Group4 : Artifact4 : Version4"));
    headers.criticalRiskHeader().click();
    table.lastComponent().shouldHave(text("Group4 : Artifact4 : Version4"));

    // CSV export with no filters
    ResponseCopyHandler responseCopyHandler = new ResponseCopyHandler("/rest/dashboard/export/componentRisks",
        testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(responseCopyHandler);
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Components Data")).click();
    DashboardPage.exportResultsLink().shouldNotBe(visible);
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page
    String exportCsv = new String(responseCopyHandler.consumeResponse());
    String[] expectedResults = {
        "Group1 : Artifact1 : Version1,1,1,0,0,0,1",
        "Group2 : Artifact2 : Version2,1,3,0,0,3,0",
        "Group3 : Artifact3 : Version3,1,7,0,7,0,0",
        "Group4 : Artifact4 : Version4,1,10,10,0,0,0"
    };
    assertComponentsCsv(exportCsv, expectedResults);

    // CSV export - filter out threat level 1
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(2, 10);
    DashboardFilters.apply();
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        "Group2 : Artifact2 : Version2,1,3,0,0,3,0",
        "Group3 : Artifact3 : Version3,1,7,0,7,0,0",
        "Group4 : Artifact4 : Version4,1,10,10,0,0,0"
    };
    assertComponentsCsv(exportCsv, expectedResults);

    // CSV export - filter out threat level 3
    DashboardFilters.policyThreatLevelFilter().slider().setValues(7, 10);
    DashboardFilters.apply();
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        "Group3 : Artifact3 : Version3,1,7,0,7,0,0",
        "Group4 : Artifact4 : Version4,1,10,10,0,0,0"
    };
    assertComponentsCsv(exportCsv, expectedResults);
  }

  private void assertComponentsCsv(String csv, String[] expectedSortedResults) {
    String[] lines = csv.split("\r\n");
    assertEquals("Component Name,Affected Apps,Total Risk,Critical,Severe,Moderate,Low", lines[0]);
    String[] results = Arrays.copyOfRange(lines, 1, lines.length);
    Arrays.sort(results);
    assertArrayEquals(Arrays.toString(results), expectedSortedResults, results);
  }

  private void addComponents(int numberOfComponents, int riskScore) {
    for (int i = 1; i <= numberOfComponents; i++) {
      addComponentWithViolation(i, riskScore);
    }
  }

  private void addComponentWithViolation(int index, int riskScore) {
    String group = "Group" + index;
    String artifact = "Artifact" + index;
    String version = "Version" + index;
    String hash = "hash" + index;
    tempEntity.newApplicationComponent(app.getId(), policyEvaluation.getStageTypeId(), hash,
        ComponentIdentifier.createMavenCoordinates(group, artifact, version));
    tempEntity.newPolicyViolation(policyEvaluation, policy, riskScore,
        PolicyThreatCategory.LICENSE, group, artifact, version, hash, FailActionType.ID);
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
