/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
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
import com.sonatype.clm.testing.functional.pages.WaiveTransitiveViolationsPopover;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.google.common.collect.ImmutableMap;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;

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
    organization = tempEntity.newOrganization("Test Org 0af5aa00a2424db19b115f70b6f873d9");
    application = tempEntity.newApplication("Test App 56770d0ec3da47b0aa8eab53d874efdb",
        "56770d0ec3da47b0aa8eab53d874efdb", organization.getId());
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId",
        new Date(1623424315000L));
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

  @Test
  public void testWaiveTransitiveViolations_InitialState() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    waiveTransitiveViolationsPopover.countsTitle()
        .shouldHave(Condition.text("3 total violations brought in by 3 components"));
    waiveTransitiveViolationsPopover.counts().shouldHaveSize(5);
    waiveTransitiveViolationsPopover.count(0).text().shouldHave(Condition.text("Critical"));
    waiveTransitiveViolationsPopover.count(0).count().shouldHave(Condition.text("1"));
    waiveTransitiveViolationsPopover.count(1).text().shouldHave(Condition.text("Severe"));
    waiveTransitiveViolationsPopover.count(1).count().shouldHave(Condition.text("1"));
    waiveTransitiveViolationsPopover.count(2).shouldNotBe(visible);
    waiveTransitiveViolationsPopover.count(3).shouldNotBe(visible);
    waiveTransitiveViolationsPopover.count(4).text().shouldHave(Condition.text("None"));
    waiveTransitiveViolationsPopover.count(4).count().shouldHave(Condition.text("1"));
    waiveTransitiveViolationsPopover.scope().shouldHave(
        Condition.text(StringUtils.capitalise(application.getType().toString()) + " - " + application.getName()));
    waiveTransitiveViolationsPopover.expiryTimesSelect().getSelectedOption().shouldHave(text("Never"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().shouldHaveSize(7);
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(0).shouldHave(text("Never"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(1).shouldHave(text("7 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(2).shouldHave(text("14 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(3).shouldHave(text("30 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(4).shouldHave(text("60 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(5).shouldHave(text("90 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(6).shouldHave(text("120 Days"));
    waiveTransitiveViolationsPopover.comments().shouldHave(Condition.text(""));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testWaiveTransitiveViolations_Toggle() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    waiveTransitiveViolationsPopover.toggle().click();
    waiveTransitiveViolationsPopover.shouldNotBe(Condition.visible);
  }

  @Test
  public void testWaiveTransitiveViolations_Cancel() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    waiveTransitiveViolationsPopover.cancelButton().click();
    waiveTransitiveViolationsPopover.shouldNotBe(Condition.visible);
  }

  @Test
  public void testWaiveTransitiveViolations_Save() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    testWaiveTransitiveViolations_Save(waiveTransitiveViolationsPopover, null, null);
  }

  @Test
  public void testWaiveTransitiveViolations_SaveWithComment() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    String comment = "someComment";
    waiveTransitiveViolationsPopover.comments().sendKeys(comment);
    testWaiveTransitiveViolations_Save(waiveTransitiveViolationsPopover, null, comment);
  }

  @Test
  public void testWaiveTransitiveViolations_SaveWithExpiry() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    waiveTransitiveViolationsPopover.expiryTimesSelect().selectOption(1);
    testWaiveTransitiveViolations_Save(waiveTransitiveViolationsPopover, getExpectedExpiryDate(7), null);
  }

  private void testWaiveTransitiveViolations_Save(
      WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover,
      Date expectedExpiryTime,
      String expectedComment)
  {
    waiveTransitiveViolationsPopover.saveButton().click();
    waiveTransitiveViolationsPopover.shouldNotBe(Condition.visible);
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getByOwnerId(application.getId());
    for (PolicyViolation policyViolation : policyViolations) {
      PolicyWaiver policyWaiver = findPolicyWaiver(policyWaivers, policyViolation);
      assertThat(policyWaiver.getExpiryTime()).isEqualTo(expectedExpiryTime);
      assertThat(policyWaiver.getComment()).isEqualTo(expectedComment);
    }
  }

  @Test
  public void testSubmitError() throws Exception {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover("hash1");
    File reportFile = testCLMServer.getCLMServer().getInstance(InsightWork.class)
        .getReportFile(application.getId(), policyEvaluation.getScanId());
    new FileCleaner().delete(reportFile.getParentFile());
    waiveTransitiveViolationsPopover.saveButton().click();
    waiveTransitiveViolationsPopover.shouldBe(Condition.visible);
    waiveTransitiveViolationsPopover.submitError().shouldBe(Condition.visible);
    waiveTransitiveViolationsPopover.saveButton().shouldHave(Condition.text("Retry"));

    ReportTestUtils.createReportFile(application.getId(), policyEvaluation.getScanId(),
        zipReportDir("/TransitiveViolationsTest/report", tempDir),
        testCLMServer.getCLMServer().getInstance(InsightWork.class));
    ReportTestUtils.createPolicyThreats(application.getId(), policyEvaluation.getScanId(),
        testCLMServer.getCLMServer().getInstance(InsightWork.class), policyViolations);
    waiveTransitiveViolationsPopover.saveButton().click();
    waiveTransitiveViolationsPopover.shouldNotBe(Condition.visible);
    new TransitiveViolationsPage().waiveTransitiveViolations().click();
    waiveTransitiveViolationsPopover.saveButton().shouldBe(Condition.visible).shouldHave(Condition.text("Save"));
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

  private WaiveTransitiveViolationsPopover visitWaivePopover() {
    return visitWaivePopover("hash1");
  }

  private WaiveTransitiveViolationsPopover visitWaivePopover(String hash) {
    TransitiveViolationsPage transitiveViolationsPage = visitPage(hash);
    transitiveViolationsPage.waiveTransitiveViolations().click();
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = new WaiveTransitiveViolationsPopover();
    waiveTransitiveViolationsPopover.shouldBe(Condition.visible);
    return waiveTransitiveViolationsPopover;
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

  private PolicyWaiver findPolicyWaiver(List<PolicyWaiver> policyWaivers, PolicyViolation policyViolation) {
    return policyWaivers.stream()
        .filter(policyWaiver -> policyWaiver.getHash() != null)
        .filter(policyWaiver -> policyWaiver.getHash().equals(policyViolation.getHash()))
        .filter(policyWaiver -> policyWaiver.getPolicyId().equals(policyViolation.getPolicyId()))
        .filter(policyWaiver -> policyWaiver.getConstraintFactsJson().equals(policyViolation.getConstraintFactsJson()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Policy waiver not found"));
  }

  private Date getExpectedExpiryDate(int daysFromNow) {
    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    return Date.from(LocalDateTime.now().plusDays(daysFromNow).withHour(23).withMinute(59).withSecond(59)
        .with(ChronoField.MILLI_OF_SECOND, 999).toInstant(offset));
  }
}
