/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.audit;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage.Row;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage.Table;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.component.MatchState;
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

    open(RepositoryReportPage.url(repoManager.getInstanceId(), repo.getPublicId()));

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
    open(RepositoryReportPage.url(repoManager.getInstanceId(), repo.getPublicId()));

    RepositoryReportPage.Summary.root().shouldBe(visible);

    RepositoryReportPage.Summary.noPolicyViolations().shouldBe(visible).shouldHave(text("0"));
    RepositoryReportPage.Summary.identifiedCount().shouldBe(visible).shouldHave(text("0"));
    RepositoryReportPage.Summary.identifiedPercent().shouldBe(visible).shouldHave(text("0"));
    RepositoryReportPage.Summary.quarantinedCount().shouldBe(visible).shouldHave(text("0"));
  }

  @Test
  public void testPolicyViolationTable() throws Exception {
    // one no violation, unknown
    RepositoryComponent unknownComponent = tempEntity.newRepositoryComponent(repo.getId(), MatchState.UNKNOWN, null);

    // one of each threat level
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
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

    // one with multiple violations
    tempEntity.newRepositoryPolicyViolation(component, 9, false, "Not in summary");

    // one with a waived violation
    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("waived", "threat", "1.0."));
    tempEntity.newRepositoryPolicyViolation(component, 10, true, "Extremely Bad");

    // one quarantined, that groups
    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("quarantined", "component", "1.0."));
    component.setQuarantineTime(new Date());
    new RepositoryComponentDAO().update(component);
    tempEntity.newRepositoryPolicyViolation(component, 10, false, "Extremely Bad");

    open(RepositoryReportPage.url(repoManager.getInstanceId(), repo.getPublicId()));

    Table.rows().shouldHaveSize(8);

    assertRow(Table.row(0), Table.criticalThreat, "Extremely Bad", "critical : threat : 1.0",
        false, false);
    assertRow(Table.row(1), Table.criticalThreat, "", "quarantined : component : 1.0", false, true);
    assertRow(Table.row(2), Table.criticalThreat, "", "waived : threat : 1.0", true, false);

    assertRow(Table.row(3), Table.criticalThreat, "Not in summary", "critical : threat : 1.0",
        false, false);

    assertRow(Table.row(4), Table.severeThreat, "Really Bad", "severe : threat : 1.0", false,
        false);

    assertRow(Table.row(5), Table.moderateThreat, "Sorta Bad", "moderate : threat : 1.0", false,
        false);

    assertRow(Table.row(6), Table.ignoredScore, "Meh", "ignored : threat : 1.0", false, false);

    // TODO should this not be the path?
    assertRow(Table.row(7), Table.noThreat, "No violations", unknownComponent.getPathname(), false, false);
  }

  private static void assertRow(Row actualRow, Condition threatLevel, String policyName, String componentName,
      boolean waived, boolean quarantined)
  {
    actualRow.policy().shouldHave(threatLevel, text(policyName));
    actualRow.component().shouldHave(text(componentName));

    if (waived) {
      actualRow.waived().shouldBe(present);
    }
    else {
      actualRow.waived().shouldNotBe(present);
    }

    if (quarantined) {
      actualRow.quarantined().shouldBe(present);
    }
    else {
      actualRow.quarantined().shouldNotBe(present);
    }
  }
}
