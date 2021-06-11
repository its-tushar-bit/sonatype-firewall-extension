/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage.ComponentDetailsHeader;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage.TransitiveViolationsRow;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage.TransitiveViolationsTable;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;

public class TransitiveViolationsTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  private Organization organization;

  private Application application;

  private List<Component> components;

  private Component component;

  private PolicyEvaluation policyEvaluation;

  private List<PolicyViolation> policyViolations;

  @Before
  public void before() throws Exception {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(ImmutableMap.of(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag(), true));
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId");
    PolicyViolation aPolicyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, tempEntity.newPolicy(application.getId(), "aPolicyX"), 10,
            PolicyThreatCategory.SECURITY,
            ComponentIdentifier.createMavenCoordinates("g", "ZtransitiveY", "v", "", "e"), "hash3", Action.ID_FAIL);
    PolicyViolation zPolicyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, tempEntity.newPolicy(application.getId(), "ZPolicy"), 5,
            PolicyThreatCategory.SECURITY, ComponentIdentifier.createMavenCoordinates("g", "btransitive", "v", "", "e"),
            "hash4", Action.ID_WARN);
    PolicyViolation bPolicyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, tempEntity.newPolicy(application.getId(), "bPolicyx", 0),
            ComponentIdentifier.createMavenCoordinates("g", "atransitivey", "v", "", "e"), "hash2");
    policyViolations = Arrays.asList(aPolicyViolation, zPolicyViolation, bPolicyViolation);
    ReportTestUtils.createReportFile(application.getId(), policyEvaluation.getScanId(),
        zipReportDir("/TransitiveViolationsTest/report", tempDir),
        testCLMServer.getCLMServer().getInstance(InsightWork.class));
    ReportTestUtils.createPolicyThreats(application.getId(), policyEvaluation.getScanId(),
        testCLMServer.getCLMServer().getInstance(InsightWork.class), policyViolations);
    File reportFile = testCLMServer.getCLMServer().getInstance(InsightWork.class)
        .getReportFile(application.getId(), policyEvaluation.getScanId());
    ReportEntry reportEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
    components = new ComponentDAO(application).getAll(null, null, reportEntry.buf, null);
    component = components.stream().filter(c -> c.getHash().equals("hash1")).findFirst().orElse(null);
  }

  @Test
  public void testInitialState() {
    refreshOrOpen(TransitiveViolationsPage.url(application.getPublicId(), policyEvaluation.getScanId(), "hash1"));
    TransitiveViolationsPage transitiveViolationsPage = visitPage();
    ComponentDetailsHeader componentDetailsHeader = transitiveViolationsPage.title();
    componentDetailsHeader.title().shouldHave(Condition.text(component.getDisplayName()));
    ElementsCollection reportInformationElements = componentDetailsHeader.reportInformationElements();
    reportInformationElements.shouldHaveSize(3);
    reportInformationElements.get(0).shouldHave(Condition.text(organization.getName()));
    reportInformationElements.get(1).shouldHave(Condition.text(application.getName()));
    reportInformationElements.get(2).shouldHave(Condition
        .text(policyEvaluation.getStageTypeId() + " Report " + getExpectedDateTime(policyEvaluation.getTime())));
    componentDetailsHeader.getElement().$(".component-details-header__tags").should(Condition.exist);
    componentDetailsHeader.tags().shouldHaveSize(1);
    componentDetailsHeader.tags().get(0).shouldHave(Condition.text("InnerSource"));
    TransitiveViolationsTable transitiveViolationsTable = transitiveViolationsPage.transitiveViolationsTable();
    transitiveViolationsTable.threatHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("-threatLevel", null, null));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testSortByThreatLevelAscending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.threatHeader().click();
    transitiveViolationsTable.threatHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("threatLevel", null, null));
  }

  @Test
  public void testSortByPolicyNameDescending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyAndActionHeader().click();
    transitiveViolationsTable.policyAndActionHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("-policyName", null, null));
  }

  @Test
  public void testSortByPolicyNameAscending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyAndActionHeader().click();
    transitiveViolationsTable.policyAndActionHeader().click();
    transitiveViolationsTable.policyAndActionHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("policyName", null, null));
  }

  @Test
  public void testSortByComponentNameDescending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentHeader().click();
    transitiveViolationsTable.componentHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("-componentName", null, null));
  }

  @Test
  public void testSortByComponentNameAscending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentHeader().click();
    transitiveViolationsTable.componentHeader().click();
    transitiveViolationsTable.componentHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("componentName", null, null));
  }

  @Test
  public void testFilterByPolicyName() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyNameFilter().sendKeys("Z");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, "Z", null));
    transitiveViolationsTable.policyNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterByPolicyName_Multiple() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyNameFilter().sendKeys("x");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, "x", null));
    transitiveViolationsTable.policyNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterByComponentName() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentNameFilter().sendKeys("Z");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, "Z"));
    transitiveViolationsTable.componentNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterByComponentName_Multiple() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentNameFilter().sendKeys("y");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, "y"));
    transitiveViolationsTable.componentNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterByPolicyNameAndComponentName() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyNameFilter().sendKeys("x");
    transitiveViolationsTable.componentNameFilter().sendKeys("z");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, "x", "z"));
    transitiveViolationsTable.policyNameFilter().sendKeys(Keys.BACK_SPACE);
    transitiveViolationsTable.componentNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterAndSort() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyNameFilter().sendKeys("x");
    transitiveViolationsTable.componentNameFilter().sendKeys("y");
    transitiveViolationsTable.threatHeader().click();
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("threatLevel", "x", "y"));
  }

  @Test
  public void testFilterToEmpty() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentNameFilter().sendKeys("l");
    transitiveViolationsTable.rows().shouldHaveSize(1);
    transitiveViolationsTable.row(1).shouldHave(Condition.text("None"));
    transitiveViolationsTable.componentNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testNotInnerSource() {
    TransitiveViolationsPage transitiveViolationsPage = visitPage("hash2");
    transitiveViolationsPage.title().getElement().$(".component-details-header__tags").shouldNot(Condition.exist);
    TransitiveViolationsTable transitiveViolationsTable = transitiveViolationsPage.transitiveViolationsTable();
    transitiveViolationsTable.rows().shouldHaveSize(1);
    transitiveViolationsTable.row(1).shouldHave(Condition.text("None"));
  }

  private TransitiveViolationsPage visitPage() {
    return visitPage("hash1");
  }

  private TransitiveViolationsPage visitPage(String hash) {
    refreshOrOpen(TransitiveViolationsPage.url(application.getPublicId(), policyEvaluation.getScanId(), hash));
    TransitiveViolationsPage transitiveViolationsPage = new TransitiveViolationsPage();
    transitiveViolationsPage.shouldBe(Condition.visible);
    return transitiveViolationsPage;
  }

  private String getExpectedDateTime(Date time) {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
  }

  private List<PolicyViolation> getExpectedPolicyViolations(
      String sortField,
      String policyNameFilter,
      String componentNameFilter)
  {
    List<PolicyViolation> result = policyViolations.stream()
        .filter(p -> policyNameFilter == null || p.getPolicyName().toLowerCase(Locale.ROOT)
            .contains(policyNameFilter.toLowerCase(Locale.ROOT)))
        .filter(p -> componentNameFilter == null || findComponent(p).getDisplayName().toLowerCase(Locale.ROOT)
            .contains(componentNameFilter.toLowerCase(Locale.ROOT)))
        .collect(Collectors.toList());
    switch (sortField == null ? "-threatLevel" : sortField) {
      case "threatLevel": {
        result.sort(Comparator.comparing(PolicyViolation::getThreatLevel));
        break;
      }
      case "-threatLevel": {
        result.sort(Comparator.comparing(PolicyViolation::getThreatLevel, Comparator.reverseOrder()));
        break;
      }
      case "policyName": {
        result.sort(Comparator.comparing(policyViolation -> policyViolation.getPolicyName().toLowerCase(Locale.ROOT)));
        break;
      }
      case "-policyName": {
        result.sort(Comparator.comparing(policyViolation -> policyViolation.getPolicyName().toLowerCase(Locale.ROOT),
            Comparator.reverseOrder()));
        break;
      }
      case "componentName": {
        result.sort(Comparator.comparing(p -> findComponent(p).getDisplayName().toLowerCase(Locale.ROOT)));
        break;
      }
      case "-componentName": {
        result.sort(Comparator.comparing(p -> findComponent(p).getDisplayName().toLowerCase(Locale.ROOT),
            Comparator.reverseOrder()));
        break;
      }
      default: {
        throw new RuntimeException("Unrecognized sortField " + sortField);
      }
    }
    return result;
  }

  private void assertRows(TransitiveViolationsTable transitiveViolationsTable, List<PolicyViolation> policyViolations) {
    transitiveViolationsTable.rows().shouldHaveSize(policyViolations.size());
    for (int row = 0; row < transitiveViolationsTable.rows().size(); row++) {
      assertRow(transitiveViolationsTable.row(row + 1), policyViolations.get(row));
    }
  }

  private void assertRow(TransitiveViolationsRow transitiveViolationsRow, PolicyViolation policyViolation) {
    transitiveViolationsRow.threat().shouldHave(Condition.text(String.valueOf(policyViolation.getThreatLevel())));
    transitiveViolationsRow.policyAndAction().shouldHave(Condition.text(policyViolation.getPolicyName()));
    if (Action.ID_FAIL.equals(policyViolation.getActionTypeId())) {
      transitiveViolationsRow.policyAndAction()
          .shouldHave(Condition.text("Failing " + policyViolation.getStageTypeId()));
    }
    else if (Action.ID_WARN.equals(policyViolation.getActionTypeId())) {
      transitiveViolationsRow.policyAndAction().shouldHave(Condition.text("Warning"));
    }
    Component component = findComponent(policyViolation);
    transitiveViolationsRow.component().shouldHave(Condition.text(component.getDisplayName()));
  }

  private Component findComponent(PolicyViolation policyViolation) {
    return components.stream().filter(c -> c.getHash().equals(policyViolation.getHash())).findFirst()
        .orElseThrow(() -> new RuntimeException("Component not found"));
  }
}
