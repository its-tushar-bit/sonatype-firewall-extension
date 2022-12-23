/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.report;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxSmallThreatCounter;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.RepositoryResultDetailPage;
import com.sonatype.clm.testing.functional.pages.RepositoryResultDetailPage.RepositoryResultTableRow;
import com.sonatype.clm.testing.functional.pages.RepositoryResultsSummaryPage;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public class RepositoryResultsSummaryTest
    extends AbstractFunctionalTest
{
  private Repository repo;

  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private final FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver()).withTimeout(Duration.ofSeconds(240)).pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }

  private void waitUntilFirewallComponentDetailsPageSpinnersGone() {
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.invisibilityOf(firewallComponentDetailsPage.getAllLoadingSpinners().get(0)));
    firewallComponentDetailsPage.getAllLoadingSpinners().shouldHave(size(0));
  }

  @BeforeClass
  public static void startup() {
    refreshOrOpen(RepositoryResultsSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() throws IOException {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");
    repo = tempEntity.newRepository(repositoryManager, "central");
    Date june1st2020 = Date.from(
        LocalDateTime.of(2020, 6, 1, 11, 0).toInstant(ZoneOffset.UTC));

    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repo.getId());
    RepositoryComponent quarantinedComponent =
        tempEntity.newRepositoryComponent(repo.getId(), "quarantined1", june1st2020, null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 1, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 2, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 3, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 4, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 5, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(quarantinedComponent, 6, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 7, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 7, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 8, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 9, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 10, false, "Policy 2", null);
  }

  @Test
  public void testRepositoryResultHeader() {

    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));

    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Repository Results for Central"));

    RepositoryResultDetailPage.indicatorRow().shouldBe(visible);
    NxSmallThreatCounter counts = RepositoryResultDetailPage.indicatorRow().counts();
    counts.all().shouldHaveSize(2);
    counts.critical().category().shouldHave(Condition.text("Critical"));
    counts.critical().count().shouldHave(Condition.text("1"));
    counts.severe().category().shouldHave(Condition.text("Severe"));
    counts.severe().count().shouldHave(Condition.text("1"));

    RepositoryResultDetailPage.indicatorRow().coverageCaptionText().shouldHave(text("2 COMPONENTS"));
    RepositoryResultDetailPage.indicatorRow().coverageCaptionSubtext()
        .shouldHave(text("100% of all components identified"));

    RepositoryResultDetailPage.indicatorRow().quarantineCaptionText().shouldHave(text("1 QUARANTINED"));
    RepositoryResultDetailPage.indicatorRow().quarantineCaptionSubtext().shouldHave(text("component"));

    eyesWatcher.eyesCheck("Repository Detail Page");
    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Repository Results for Central"));

  }

  @Test
  public void testRepositoryResultTableAndPagination() {
    RepositoryResultDetailPage page = new RepositoryResultDetailPage();
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.table().header().threat().shouldHave(text("THREAT"));
    RepositoryResultDetailPage.table().header().policy().shouldHave(text("POLICY"));
    RepositoryResultDetailPage.table().header().quarantined().shouldHave(text("QUARANTINED"));
    RepositoryResultDetailPage.table().header().component().shouldHave(text("COMPONENT"));

    RepositoryResultDetailPage.table().rows().shouldHaveSize(12);
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Test Policy", "", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Policy 2", "", "g : a : v");

    SelenideElement leftPagination = page.paginationButtons().get(0);
    SelenideElement rightPagination = page.paginationButtons().get(1);
    leftPagination.shouldNotBe(visible);
    rightPagination.shouldBe(visible).click();
    RepositoryResultDetailPage.table().rows().shouldHaveSize(12);
    testRow(RepositoryResultDetailPage.table().row(11), "6", "Test Policy", "2020-06-01", "g : a : v");

    leftPagination.shouldBe(visible);
    rightPagination.shouldBe(visible).click();
    RepositoryResultDetailPage.table().rows().shouldHaveSize(5);
    testRow(RepositoryResultDetailPage.table().row(0), "5", "Test Policy", "", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(4), "1", "Test Policy", "", "g : a : v");

    rightPagination.shouldNotBe(visible);
    leftPagination.shouldBe(visible).click();
    RepositoryResultDetailPage.table().rows().shouldHaveSize(12);
    testRow(RepositoryResultDetailPage.table().row(11), "6", "Test Policy", "2020-06-01", "g : a : v");

    leftPagination.shouldBe(visible);
    rightPagination.shouldBe(visible);
  }

  @Test
  public void testRepositoryResultSorting() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));

    RepositoryResultDetailPage.table().rows().shouldHaveSize(12);
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Test Policy", "", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Policy 2", "", "g : a : v");

    RepositoryResultDetailPage.table().header().threat().click();
    testRow(RepositoryResultDetailPage.table().row(0), "1", "Test Policy", "", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(11), "10", "Policy 2", "", "g : a : v");

    RepositoryResultDetailPage.table().header().policy().click();
    testRow(RepositoryResultDetailPage.table().row(0), "3", "Policy 2", "", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(11), "10", "Policy 2", "", "g : a : v");

    RepositoryResultDetailPage.table().header().quarantined().click();
    testRow(RepositoryResultDetailPage.table().row(0), "6", "Test Policy", "2020-06-01", "g : a : v");
  }

  @Test
  public void testRepositoryResultTextFiltering() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    RepositoryResultDetailPage.table().rows().shouldHaveSize(12);
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Test Policy", "", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Policy 2", "", "g : a : v");
    RepositoryResultDetailPage.table().policyName().input().shouldBe(visible);
    RepositoryResultDetailPage.table().policyName().input().sendKeys("Test");

    RepositoryResultDetailPage.table().rows().shouldHaveSize(9);
    SelenideElement leftPagination = page.paginationButtons().get(0);
    SelenideElement rightPagination = page.paginationButtons().get(1);
    leftPagination.shouldNotBe(visible);
    rightPagination.shouldNotBe(visible);

    RepositoryResultDetailPage.table().policyName().input().sendKeys("");
    RepositoryResultDetailPage.table().componentName().input().sendKeys("nothing");
    RepositoryResultDetailPage.table().rows().shouldHaveSize(1);
  }

  @Test
  public void testRepositoryResultTextFilterPopover() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.filterPopover().shouldNotBe(visible);
    RepositoryResultDetailPage.filterPopoverButton().shouldBe(visible).click();
    RepositoryResultDetailPage.filterPopover().shouldBe(visible);
    RepositoryResultDetailPage.filterPopover().closeButton().shouldBe(visible).click();
    RepositoryResultDetailPage.filterPopover().shouldNotBe(visible);
    RepositoryResultDetailPage.filterPopover().shouldNotBe(visible);
    RepositoryResultDetailPage.filterPopover().shouldNotBe(visible);
  }

  @Test
  public void testRepositoryResultReEvaluateButton() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Repository Results for Central"));
    RepositoryResultDetailPage.reEvaluateReportButton().click();
    RepositoryResultDetailPage.reEvaluateModalButton().shouldBe(visible);
    RepositoryResultDetailPage.reEvaluateModalButton().click();
    RepositoryResultDetailPage.reEvaluateModalButton().shouldNotBe(visible);
    FormMask.seeAndWaitForDismissal();
  }

  @Test
  public void testRepositoryResultReEvaluateCancelModalButton() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Repository Results for Central"));
    RepositoryResultDetailPage.reEvaluateReportButton().click();
    RepositoryResultDetailPage.reEvaluateModalCancelButton().shouldBe(visible);
    RepositoryResultDetailPage.reEvaluateModalCancelButton().click();
    RepositoryResultDetailPage.reEvaluateModalCancelButton().shouldNotBe(visible);
  }

  @Test
  public void testClickOnTableRowRedirectsToComponentDetailsPage() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Repository Results for Central"));
    RepositoryResultTableRow row = RepositoryResultDetailPage.table().row(1);
    row.click();

    waitUntilFirewallComponentDetailsPageSpinnersGone();
    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);

    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);
    firewallComponentDetailsPage.getViewCoordinatesButton().click();
    firewallComponentDetailsPage.getComponentCoordinatesPopOver().shouldBe(visible);
    firewallComponentDetailsPage.getComponentCoordinatesPopOverData(0).shouldHave(text("maven"));
    firewallComponentDetailsPage.getComponentCoordinatesPopOverData(1).shouldHave(text("g"));
    firewallComponentDetailsPage.getComponentCoordinatesPopOverData(2).shouldHave(text("a"));
    firewallComponentDetailsPage.getComponentCoordinatesPopOverData(3).shouldHave(text("v"));
  }

  private void testRow(RepositoryResultTableRow row,
                       String threat,
                       String policy,
                       String quarantined,
                       String component)
  {
    row.threat().shouldHave(text(threat));
    row.policy().shouldHave(text(policy));
    row.quarantined().shouldHave(text(quarantined));
    row.component().shouldHave(text(component));
  }
}
