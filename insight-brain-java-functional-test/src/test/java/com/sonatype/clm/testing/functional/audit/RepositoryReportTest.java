/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.audit;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LabelsCIP;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage.Filter;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage.Row;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage.Table;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import com.codeborne.selenide.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.present;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;

public class RepositoryReportTest
    extends AbstractFunctionalTest
{
  private RepositoryManager repoManager;

  private Repository repo;

  private String criticalComponentHash;

  @BeforeClass
  public static void startup() {
    open(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    repoManager = tempEntity.newRepositoryManager();
    // repositoryPublicId has a character requiring encoding
    repo = tempEntity.newRepository(repoManager, "ce&ntral");
  }

  @Test
  public void testSummary() throws Exception {
    tempEntity.newRepositoryComponent(repo.getId());
    tempEntity.newRepositoryComponent(repo.getId(), MatchState.UNKNOWN, null);

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 3, "3", null);

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 5, "5", null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 6, "6", null);

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 8, "8", null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 9, "9", null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 10, "10", null);

    tempEntity.newRepositoryComponent(repo.getId(), "quarantined1", new Date(), null);
    tempEntity.newRepositoryComponent(repo.getId(), "quarantined2", new Date(), null);

    open(RepositoryReportPage.url(repo.getId()));

    RepositoryReportPage.Summary.root().shouldBe(visible);

    RepositoryReportPage.Summary.moderateCount().shouldBe(visible).shouldHave(text("1"));
    RepositoryReportPage.Summary.severeCount().shouldBe(visible).shouldHave(text("2"));
    RepositoryReportPage.Summary.criticalCount().shouldBe(visible).shouldHave(text("3"));
    RepositoryReportPage.Summary.violatingComponentsCount().shouldBe(visible).shouldHave(text("6"));
    RepositoryReportPage.Summary.quarantinedCount().shouldBe(visible).shouldHave(text("2"));

    RepositoryReportPage.Summary.identifiedCount().shouldBe(visible).shouldHave(text("3"));
    RepositoryReportPage.Summary.identifiedPercent().shouldBe(visible).shouldHave(text("75"));
  }

  @Test
  public void testSummary_Empty() throws Exception {
    open(RepositoryReportPage.url(repo.getId()));

    RepositoryReportPage.Summary.root().shouldBe(visible);

    RepositoryReportPage.Summary.noPolicyViolations().shouldBe(visible).shouldHave(text("0"));
    RepositoryReportPage.Summary.identifiedCount().shouldBe(visible).shouldHave(text("0"));
    RepositoryReportPage.Summary.identifiedPercent().shouldBe(visible).shouldHave(text("0"));
    RepositoryReportPage.Summary.quarantinedCount().shouldBe(visible).shouldHave(text("0"));
  }

  @Test
  public void testPage() throws Exception {
    // one no violation, unknown
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.UNKNOWN, null);
    UNKNOWN = new ExpectedRow(Table.noThreat, "No violations", component.getPathname(), false, false);

    // one of each threat level
    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("ignored", "threat", "1.0."));
    tempEntity.newRepositoryPolicyViolation(component, 1, false, "Meh");

    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("moderate", "threat", "1.0."));
    tempEntity.newRepositoryPolicyViolation(component, 3, false, "Sorta Bad");

    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("severe", "threat", "1.0."));
    tempEntity.newRepositoryPolicyViolation(component, 6, false, "Really Bad");

    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("critical", "threat", "1.0."));
    tempEntity.newRepositoryPolicyViolation(component, 10, false, "Extremely Bad");
    criticalComponentHash = component.getHash();

    // one with multiple violations
    tempEntity.newRepositoryPolicyViolation(component, 9, false, "Not in summary");

    // one quarantined, that groups
    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("quarantined", "component", "1.0."));
    component.setQuarantineTime(new Date());
    new RepositoryComponentDAO().update(component);
    tempEntity.newRepositoryPolicyViolation(component, 10, false, "Extremely Bad");

    open(RepositoryReportPage.url(repo.getId()));

    testReportSummary();

    // Default filter settings
    Filter.allMatchState().shouldBe(Filter.active);
    Filter.summaryViolations().shouldBe(Filter.active);

    assertRows(CRITICAL_ROW, QUARANTINED, SEVERE_ROW, MODERATE_ROW, IGNORED_ROW, UNKNOWN);

    testExactMatchesFilter();
    testUnknownMatchesFilter();

    testAllViolationsFilter();
    testQuarantinedFilter();

    testLabelsCIP();
  }

  private void testLabelsCIP() {
    Label applied = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "El Junko", Color.blue);
    tempEntity.newComponentLabel(Organization.ROOT_ORGANIZATION_ID, applied.getId(), criticalComponentHash);

    // open CIP
    RepositoryReportPage.Table.row(0).component().click();
    RepositoryReportPage.Table.cip().shouldBe(visible);

    RepositoryReportPage.Table.cipTab("Labels").click();

    LabelsCIP.appliedLabels().shouldHaveSize(1);
    LabelsCIP.appliedLabel(0).shouldHave(text("El Junko"), LabelsCIP.Label.color(Color.blue));
    LabelsCIP.availableLabelsContainer().shouldNotBe(present);

    // close CIP
    RepositoryReportPage.Table.row(0).component().click();
    RepositoryReportPage.Table.cip().shouldNotBe(visible);
  }

  private void testReportSummary() {
    RepositoryReportPage.Summary.root().shouldBe(visible);

    RepositoryReportPage.Summary.moderateCount().shouldBe(visible).shouldHave(text("1"));
    RepositoryReportPage.Summary.severeCount().shouldBe(visible).shouldHave(text("1"));
    RepositoryReportPage.Summary.criticalCount().shouldBe(visible).shouldHave(text("2"));
    RepositoryReportPage.Summary.violatingComponentsCount().shouldBe(visible).shouldHave(text("4"));

    RepositoryReportPage.Summary.identifiedCount().shouldBe(visible).shouldHave(text("5"));
    RepositoryReportPage.Summary.identifiedPercent().shouldBe(visible).shouldHave(text("83"));
  }

  private void testUnknownMatchesFilter() {
    Filter.unknownMatchStateButton().click();
    Filter.unknownMatchState().shouldBe(Filter.active);

    assertRows(UNKNOWN);

    resetFilter();
  }

  private void testExactMatchesFilter() {
    Filter.exactMatchStateButton().click();
    Filter.exactMatchState().shouldBe(Filter.active);

    assertRows(CRITICAL_ROW, QUARANTINED, SEVERE_ROW, MODERATE_ROW, IGNORED_ROW);

    resetFilter();
  }

  private void testAllViolationsFilter() {
    Filter.allViolationsButton().click();
    Filter.allViolations().shouldBe(Filter.active);

    assertRows(CRITICAL_ROW, QUARANTINED, CRITICAL_ROW_SECONDARY, SEVERE_ROW, MODERATE_ROW, IGNORED_ROW,
        UNKNOWN);
    resetFilter();
  }

  private void testQuarantinedFilter() {
    Filter.quarantinedViolationsButton().click();
    Filter.quarantinedViolations().shouldBe(Filter.active);

    assertRows(QUARANTINED);
    resetFilter();
  }

  private void resetFilter() {
    Filter.allMatchStateButton().click();
    Filter.summaryViolationsButton().click();
    Filter.allMatchState().shouldBe(Filter.active);
    Filter.summaryViolations().shouldBe(Filter.active);
  }

  private static void assertRows(ExpectedRow... expectedRows) {
    Table.rows().shouldHaveSize(expectedRows.length);

    String previousPolicyName = null;
    for (int i = 0; i < expectedRows.length; i++) {
      assertRow(Table.row(i), expectedRows[i], expectedRows[i].policyName.equals(previousPolicyName));
      previousPolicyName = expectedRows[i].policyName;
    }
  }

  private static void assertRow(Row actualRow, ExpectedRow expectedRow, boolean shouldBeGrouped) {
    actualRow.policy().shouldHave(expectedRow.threatLevel);
    if (shouldBeGrouped) {
      actualRow.policy().shouldHave(text(""));
    }
    else {
      actualRow.policy().shouldHave(text(expectedRow.policyName));
    }
    actualRow.component().shouldHave(text(expectedRow.componentName));

    if (expectedRow.waived) {
      actualRow.waived().shouldBe(present);
    }
    else {
      actualRow.waived().shouldNotBe(present);
    }

    if (expectedRow.quarantined) {
      actualRow.quarantined().shouldBe(present);
    }
    else {
      actualRow.quarantined().shouldNotBe(present);
    }
  }

  private ExpectedRow UNKNOWN;

  private final ExpectedRow QUARANTINED = new ExpectedRow(Table.criticalThreat, "Extremely Bad",
      "quarantined : component : 1.0", false, true);

  private final ExpectedRow CRITICAL_ROW = new ExpectedRow(Table.criticalThreat, "Extremely Bad",
      "critical : threat : 1.0", false, false);

  private final ExpectedRow CRITICAL_ROW_SECONDARY = new ExpectedRow(Table.criticalThreat, "Not In Summary",
      "critical : threat : 1.0", false, false);

  private final ExpectedRow SEVERE_ROW = new ExpectedRow(Table.severeThreat, "Really Bad", "severe : threat : 1.0",
      false, false);

  private final ExpectedRow MODERATE_ROW = new ExpectedRow(Table.moderateThreat, "Sorta Bad", "moderate : threat : 1.0",
      false, false);

  private final ExpectedRow IGNORED_ROW = new ExpectedRow(Table.ignoredScore, "Meh", "ignored : threat : 1.0", false,
      false);

  private static class ExpectedRow
  {
    Condition threatLevel;

    String policyName;

    String componentName;

    boolean waived;

    boolean quarantined;

    ExpectedRow(Condition threatLevel, String policyName, String componentName, boolean waived, boolean quarantined) {
      this.threatLevel = threatLevel;
      this.policyName = policyName;
      this.componentName = componentName;
      this.waived = waived;
      this.quarantined = quarantined;
    }
  }
}
