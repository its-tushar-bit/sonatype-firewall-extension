/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.audit;

import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
}
