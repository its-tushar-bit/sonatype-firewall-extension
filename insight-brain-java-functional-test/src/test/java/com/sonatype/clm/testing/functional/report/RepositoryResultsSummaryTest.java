/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.report;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxSmallThreatCounter;
import com.sonatype.clm.testing.functional.elements.NxTreeViewMultiSelect;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.clm.testing.functional.pages.RepositoryResultDetailPage;
import com.sonatype.clm.testing.functional.pages.RepositoryResultDetailPage.RepositoryFilterPopover;
import com.sonatype.clm.testing.functional.pages.RepositoryResultDetailPage.RepositoryResultTable;
import com.sonatype.clm.testing.functional.pages.RepositoryResultDetailPage.RepositoryResultTableRow;
import com.sonatype.clm.testing.functional.pages.RepositoryResultsSummaryPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

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

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.not;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.sonatype.clm.testing.functional.brain.FirewallComponentDetailsPageTest.toLicenseDTO;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.ACTIVE;

public class RepositoryResultsSummaryTest
    extends AbstractFunctionalTest
{
  private MultiLicenseDAO multiLicenseDAO;

  private RepositoryDAO repositoryDAO;

  private Repository repo;

  private RepositoryManager repositoryManager;

  private final FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver()).withTimeout(Duration.ofSeconds(240)).pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }

  private void waitUntilFirewallComponentDetailsPageSpinnersGone() {
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.invisibilityOf(firewallComponentDetailsPage.getAllLoadingSpinners().get(0)));
  }

  @BeforeClass
  public static void startup() {
    refreshOrOpen(RepositoryResultsSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    multiLicenseDAO = lookup(MultiLicenseDAO.class);
    repositoryDAO = lookup(RepositoryDAO.class);

    repositoryManager = tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");
    repo = tempEntity.newRepository(repositoryManager, "central");
    // NEXUS-50206: Enable quarantine so quarantined components are counted
    repo.setQuarantineEnabled(true);
    repositoryDAO.update(repo);
    Instant instant = LocalDateTime.of(2020, 6, 1, 11, 0).atZone(ZoneId.systemDefault()).toInstant();
    Date june1st2020 = Date.from(instant);

    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repo.getId());
    RepositoryComponent quarantinedComponent =
        tempEntity.newRepositoryComponent(repo.getId(), "quarantined1", june1st2020, null);

    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 1, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 2, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 3, false, "Policy 2", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 4, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 5, false, "Test Policy", null);
    tempEntity.newRepositoryPolicyViolation(quarantinedComponent, 6, false, "Test Policy", Action.ID_FAIL);
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

    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Central Repository Results"));

    RepositoryResultDetailPage.indicatorRow().shouldBe(visible);
    NxSmallThreatCounter counts = RepositoryResultDetailPage.indicatorRow().counts();
    counts.critical().category().shouldHave(Condition.text("Critical"));
    counts.critical().count().shouldHave(Condition.text("21"));
    counts.severe().category().shouldHave(Condition.text("Severe"));
    counts.severe().count().shouldHave(Condition.text("5"));
    counts.moderate().category().shouldHave(Condition.text("Moderate"));
    counts.moderate().count().shouldHave(Condition.text("2"));

    RepositoryResultDetailPage.indicatorRow().coverageCaptionText().shouldHave(text("2 COMPONENTS"));
    RepositoryResultDetailPage.indicatorRow().coverageCaptionSubtext()
        .shouldHave(text("100% of all components identified"));

    RepositoryResultDetailPage.indicatorRow().quarantineCaptionText().shouldHave(text("1 QUARANTINED"));
    RepositoryResultDetailPage.indicatorRow().quarantineCaptionSubtext().shouldHave(text("component"));

    // eyesWatcher.eyesCheck("Repository Detail Page"); https://sonatype.atlassian.net/browse/CLM-30559
    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Central Repository Results"));

  }

  @Test
  public void testRepositoryResultTableAndPagination() {
    RepositoryResultDetailPage page = new RepositoryResultDetailPage();
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();
    RepositoryResultDetailPage.table().header().threat().shouldHave(text("THREAT"));
    RepositoryResultDetailPage.table().header().policy().shouldHave(text("POLICY"));
    RepositoryResultDetailPage.table().header().quarantined().shouldHave(text("QUARANTINE TIME"));
    RepositoryResultDetailPage.table().header().component().shouldHave(text("COMPONENT"));

    RepositoryResultDetailPage.table().rows().shouldHave(size(12));
    testRow(RepositoryResultDetailPage.table().row(0), "6", "Test Policy", "2020-06-01", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Policy 2", "", "g : a : v");

    SelenideElement leftPagination = page.paginationButtons().get(0);
    SelenideElement rightPagination = page.paginationButtons().get(1);
    leftPagination.shouldNotBe(visible);
    rightPagination.shouldBe(visible).click();
    RepositoryResultDetailPage.table().rows().shouldHave(size(12));
    testRow(RepositoryResultDetailPage.table().row(11), "7", "Test Policy", "", "g : a : v");

    leftPagination.shouldBe(visible);
    rightPagination.shouldBe(visible).click();
    RepositoryResultDetailPage.table().rows().shouldHave(size(5));
    testRow(RepositoryResultDetailPage.table().row(0), "5", "Test Policy", "", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(4), "1", "Test Policy", "", "g : a : v");

    rightPagination.shouldNotBe(visible);
    leftPagination.shouldBe(visible).click();
    RepositoryResultDetailPage.table().rows().shouldHave(size(12));
    testRow(RepositoryResultDetailPage.table().row(11), "7", "Test Policy", "", "g : a : v");

    leftPagination.shouldBe(visible);
    rightPagination.shouldBe(visible);
  }

  @Test
  public void testRepositoryResultSorting() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();

    RepositoryResultDetailPage.table().rows().shouldHave(size(12));

    testRow(RepositoryResultDetailPage.table().row(0), "6", "Test Policy", "2020-06-01", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Policy 2", "", "g : a : v");

    RepositoryResultDetailPage.table().header().threat().click();
    testRow(RepositoryResultDetailPage.table().row(0), "1", "Test Policy", "", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(11), "10", "Policy 2", "", "g : a : v");

    RepositoryResultDetailPage.table().header().policy().click();
    testRow(RepositoryResultDetailPage.table().row(0), "1", "Test Policy", "", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(11), "10", "Policy 2", "", "g : a : v");
  }

  @Test
  public void testRepositoryResultTableAggregate() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();
    SelenideElement leftPagination = page.paginationButtons().get(0);
    SelenideElement rightPagination = page.paginationButtons().get(1);

    leftPagination.shouldNotBe(visible);
    rightPagination.shouldNotBe(visible);

    RepositoryResultDetailPage.table().rows().shouldHave(size(2));

    testRow(RepositoryResultDetailPage.table().row(0), "6", "Test Policy", "2020-06-01", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Test Policy", "", "g : a : v");
  }

  @Test
  public void testRepositoryResultTextFiltering() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();
    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    RepositoryResultDetailPage.table().rows().shouldHave(size(12));
    testRow(RepositoryResultDetailPage.table().row(0), "6", "Test Policy", "2020-06-01", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Policy 2", "", "g : a : v");
    RepositoryResultDetailPage.table().policyName().input().shouldBe(visible);
    RepositoryResultDetailPage.table().policyName().input().sendKeys("Test");
    RepositoryResultDetailPage.table().policyNameClearFilterButton().should(visible);

    RepositoryResultDetailPage.table().rows().shouldHave(size(9));
    SelenideElement leftPagination = page.paginationButtons().get(0);
    SelenideElement rightPagination = page.paginationButtons().get(1);
    leftPagination.shouldNotBe(visible);
    rightPagination.shouldNotBe(visible);

    RepositoryResultDetailPage.table().policyNameClearFilterButton().click();
    RepositoryResultDetailPage.table().componentName().input().sendKeys("nothing");
    RepositoryResultDetailPage.table().rows().shouldHave(size(1));

    RepositoryResultDetailPage.table().componentNameClearFilterButton().should(visible);
    RepositoryResultDetailPage.table().componentNameClearFilterButton().click();
    RepositoryResultDetailPage.table().rows().shouldHave(size(12));

    RepositoryResultDetailPage.table().quarantineTime().input().sendKeys("2020-06-01");
    RepositoryResultDetailPage.table().rows().shouldHave(size(1));
    RepositoryResultDetailPage.table().quarantineTimeClearFilterButton().should(visible);
    RepositoryResultDetailPage.table().quarantineTimeClearFilterButton().click();
    RepositoryResultDetailPage.table().rows().shouldHave(size(12));
  }

  @Test
  public void testRepositoryResultTextFilteringAggregate() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));

    RepositoryResultDetailPage.table().rows().shouldHave(size(2));
    testRow(RepositoryResultDetailPage.table().row(0), "6", "Test Policy", "2020-06-01", "g : a : v");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Test Policy", "", "g : a : v");

    RepositoryResultDetailPage.table().componentName().input().sendKeys("nothing");
    RepositoryResultDetailPage.table().rows().shouldHave(size(1));

    RepositoryResultDetailPage.table().componentNameClearFilterButton().should(visible);
    RepositoryResultDetailPage.table().componentNameClearFilterButton().click();
    RepositoryResultDetailPage.table().rows().shouldHave(size(2));

    RepositoryResultDetailPage.table().quarantineTime().input().sendKeys("2020-06-01");
    RepositoryResultDetailPage.table().rows().shouldHave(size(1));
    RepositoryResultDetailPage.table().quarantineTimeClearFilterButton().should(visible);
    RepositoryResultDetailPage.table().quarantineTimeClearFilterButton().click();
    RepositoryResultDetailPage.table().rows().shouldHave(size(2));
  }

  @Test
  public void testRepositoryResultReEvaluateButton() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Central Repository Results"));
    RepositoryResultDetailPage.reEvaluateReportButton().click();
    RepositoryResultDetailPage.reEvaluateModalButton().shouldBe(visible);
    RepositoryResultDetailPage.reEvaluateModalButton().click();
    RepositoryResultDetailPage.reEvaluateModalButton().shouldNotBe(visible);
    FormMask.seeAndWaitForDismissal();
  }

  @Test
  public void testRepositoryResultReEvaluateCancelModalButton() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Central Repository Results"));
    RepositoryResultDetailPage.reEvaluateReportButton().click();
    RepositoryResultDetailPage.reEvaluateModalCancelButton().shouldBe(visible);
    RepositoryResultDetailPage.reEvaluateModalCancelButton().click();
    RepositoryResultDetailPage.reEvaluateModalCancelButton().shouldNotBe(visible);
  }

  @Test
  public void testClickOnTableRowRedirectsToComponentDetailsPage() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("Central Repository Results"));
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

  @Test
  public void testFiltersRepositoryResultsSummaryPage() {
    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();

    // Sanity Check
    RepositoryResultDetailPage.table().rows().shouldHave(size(12));
    RepositoryResultTable repositoryResultsTable = new RepositoryResultTable();

    // Filter by Policy Name
    // Doesn't filter. Filter invalid
    repositoryResultsTable.policyName().input().sendKeys("T");
    RepositoryResultDetailPage.table().rows().shouldHave(size(12));
    repositoryResultsTable.policyNameClearFilterButton().click();

    // Filter results by policy name
    repositoryResultsTable.policyName().input().sendKeys("Test");
    RepositoryResultDetailPage.table().rows().shouldHave(size(9));
    repositoryResultsTable.policyNameClearFilterButton().click();
  }

  @Test
  public void testDefaultSorting_QuarantinedComponentsWithPolicyViolations() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();

    // Quarantined components should be at the top
    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-10",
        "groupId10 : artifactId10 : jar : classifier10 : version10");
    testRow(RepositoryResultDetailPage.table().row(1), "9", "Policy Threat Level 9", "2020-06-09",
        "groupId9 : artifactId9 : jar : classifier9 : version9");
    testRow(RepositoryResultDetailPage.table().row(2), "8", "Policy Threat Level 8", "2020-06-08",
        "groupId8 : artifactId8 : jar : classifier8 : version8");
    testRow(RepositoryResultDetailPage.table().row(3), "7", "Policy Threat Level 7", "2020-06-07",
        "groupId7 : artifactId7 : jar : classifier7 : version7");
    testRow(RepositoryResultDetailPage.table().row(4), "6", "Policy Threat Level 6", "2020-06-06",
        "groupId6 : artifactId6 : jar : classifier6 : version6");
    testRow(RepositoryResultDetailPage.table().row(5), "10", "Policy Threat Level 10", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(6), "9", "Policy Threat Level 9", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(7), "5", "Policy Threat Level 5", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(8), "5", "Policy Threat Level 5B", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(9), "10", "Policy Threat Level 10", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(10), "9", "Policy Threat Level 9", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(11), "4", "Policy Threat Level 4", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");

    SelenideElement rightPagination = page.paginationButtons().get(1);
    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "4", "Policy Threat Level 4B", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Policy Threat Level 10", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(2), "9", "Policy Threat Level 9", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(3), "3", "Policy Threat Level 3", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(4), "3", "Policy Threat Level 3B", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(5), "10", "Policy Threat Level 10", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(6), "9", "Policy Threat Level 9", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(7), "2", "Policy Threat Level 2", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(8), "2", "Policy Threat Level 2B", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(9), "10", "Policy Threat Level 10", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(10), "9", "Policy Threat Level 9", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1B", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
  }

  @Test
  public void testDefaultSorting_QuarantinedComponentsWithPolicyViolations_Aggregate() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));

    // Quarantined components should be at the top
    RepositoryResultDetailPage page = new RepositoryResultDetailPage();
    SelenideElement rightPagination = page.paginationButtons().get(1);

    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-10",
        "groupId10 : artifactId10 : jar : classifier10 : version10");
    testRow(RepositoryResultDetailPage.table().row(1), "9", "Policy Threat Level 9", "2020-06-09",
        "groupId9 : artifactId9 : jar : classifier9 : version9");
    testRow(RepositoryResultDetailPage.table().row(2), "8", "Policy Threat Level 8", "2020-06-08",
        "groupId8 : artifactId8 : jar : classifier8 : version8");
    testRow(RepositoryResultDetailPage.table().row(3), "7", "Policy Threat Level 7", "2020-06-07",
        "groupId7 : artifactId7 : jar : classifier7 : version7");
    testRow(RepositoryResultDetailPage.table().row(4), "6", "Policy Threat Level 6", "2020-06-06",
        "groupId6 : artifactId6 : jar : classifier6 : version6");
    testRow(RepositoryResultDetailPage.table().row(5), "10", "Policy Threat Level 10", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(6), "10", "Policy Threat Level 10", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(7), "10", "Policy Threat Level 10", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(8), "10", "Policy Threat Level 10", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(9), "10", "Policy Threat Level 10", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(10), "1", "Policy Threat Level 1", "",
        "groupId11 : artifactId11 : jar : classifier11 : version11");
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "",
        "groupId12 : artifactId12 : jar : classifier12 : version12");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId13 : artifactId13 : jar : classifier13 : version13");
    testRow(RepositoryResultDetailPage.table().row(1), "1", "Policy Threat Level 1", "",
        "groupId14 : artifactId14 : jar : classifier14 : version14");
    testRow(RepositoryResultDetailPage.table().row(2), "1", "Policy Threat Level 1", "",
        "groupId15 : artifactId15 : jar : classifier15 : version15");
    testRow(RepositoryResultDetailPage.table().row(3), "1", "Policy Threat Level 1", "",
        "groupId16 : artifactId16 : jar : classifier16 : version16");
    testRow(RepositoryResultDetailPage.table().row(4), "0", "No Violations", "",
        "groupId20 : artifactId20 : jar : classifier20 : version20");
    testRow(RepositoryResultDetailPage.table().row(5), "0", "No Violations", "",
        "groupId21 : artifactId21 : jar : classifier21 : version21");
    testRow(RepositoryResultDetailPage.table().row(6), "0", "No Violations", "",
        "groupId22 : artifactId22 : jar : classifier22 : version22");
    testRow(RepositoryResultDetailPage.table().row(7), "0", "No Violations", "",
        "groupId23 : artifactId23 : jar : classifier23 : version23");
    testRow(RepositoryResultDetailPage.table().row(8), "0", "No Violations", "",
        "groupId24 : artifactId24 : jar : classifier24 : version24");
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId25 : artifactId25 : jar : classifier25 : version25");
  }

  @Test
  public void testDefaultSorting_NotQuarantinedComponentsWithPolicyViolations() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    SelenideElement rightPagination = page.paginationButtons().get(1);
    rightPagination.click();
    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(1), "1", "Policy Threat Level 1", "",
        "groupId11 : artifactId11 : jar : classifier11 : version11");

    testRow(RepositoryResultDetailPage.table().row(2), "1", "Policy Threat Level 1", "",
        "groupId12 : artifactId12 : jar : classifier12 : version12");

    testRow(RepositoryResultDetailPage.table().row(3), "1", "Policy Threat Level 1", "",
        "groupId13 : artifactId13 : jar : classifier13 : version13");

    testRow(RepositoryResultDetailPage.table().row(4), "1", "Policy Threat Level 1", "",
        "groupId14 : artifactId14 : jar : classifier14 : version14");

    testRow(RepositoryResultDetailPage.table().row(5), "1", "Policy Threat Level 1", "",
        "groupId15 : artifactId15 : jar : classifier15 : version15");

    testRow(RepositoryResultDetailPage.table().row(6), "1", "Policy Threat Level 1", "",
        "groupId16 : artifactId16 : jar : classifier16 : version16");
  }

  @Test
  public void testDefaultSorting_NotQuarantinedComponentsWithPolicyViolations_Aggregate() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    SelenideElement rightPagination = page.paginationButtons().get(1);

    testRow(RepositoryResultDetailPage.table().row(10), "1", "Policy Threat Level 1", "",
        "groupId11 : artifactId11 : jar : classifier11 : version11");

    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "",
        "groupId12 : artifactId12 : jar : classifier12 : version12");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId13 : artifactId13 : jar : classifier13 : version13");

    testRow(RepositoryResultDetailPage.table().row(1), "1", "Policy Threat Level 1", "",
        "groupId14 : artifactId14 : jar : classifier14 : version14");

    testRow(RepositoryResultDetailPage.table().row(2), "1", "Policy Threat Level 1", "",
        "groupId15 : artifactId15 : jar : classifier15 : version15");

    testRow(RepositoryResultDetailPage.table().row(3), "1", "Policy Threat Level 1", "",
        "groupId16 : artifactId16 : jar : classifier16 : version16");
  }

  @Test
  public void testDefaultSorting_ComponentsWithoutPolicyViolations() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    SelenideElement rightPagination = page.paginationButtons().get(1);
    rightPagination.click();
    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(7), "0", "No Violations", "",
        "groupId20 : artifactId20 : jar : classifier20 : version20");
    testRow(RepositoryResultDetailPage.table().row(8), "0", "No Violations", "",
        "groupId21 : artifactId21 : jar : classifier21 : version21");
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId22 : artifactId22 : jar : classifier22 : version22");
    testRow(RepositoryResultDetailPage.table().row(10), "0", "No Violations", "",
        "groupId23 : artifactId23 : jar : classifier23 : version23");
    testRow(RepositoryResultDetailPage.table().row(11), "0", "No Violations", "",
        "groupId24 : artifactId24 : jar : classifier24 : version24");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "0", "No Violations", "",
        "groupId25 : artifactId25 : jar : classifier25 : version25");
  }

  @Test
  public void testDefaultSorting_ComponentsWithoutPolicyViolations_Aggregate() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    SelenideElement rightPagination = page.paginationButtons().get(1);

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(4), "0", "No Violations", "",
        "groupId20 : artifactId20 : jar : classifier20 : version20");
    testRow(RepositoryResultDetailPage.table().row(5), "0", "No Violations", "",
        "groupId21 : artifactId21 : jar : classifier21 : version21");
    testRow(RepositoryResultDetailPage.table().row(6), "0", "No Violations", "",
        "groupId22 : artifactId22 : jar : classifier22 : version22");
    testRow(RepositoryResultDetailPage.table().row(7), "0", "No Violations", "",
        "groupId23 : artifactId23 : jar : classifier23 : version23");
    testRow(RepositoryResultDetailPage.table().row(8), "0", "No Violations", "",
        "groupId24 : artifactId24 : jar : classifier24 : version24");
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId25 : artifactId25 : jar : classifier25 : version25");
  }

  @Test
  public void testDefaultSorting_MultipleViolationsSameComponent() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    testRow(RepositoryResultDetailPage.table().row(5), "10", "Policy Threat Level 10", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(6), "9", "Policy Threat Level 9", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(7), "5", "Policy Threat Level 5", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(8), "5", "Policy Threat Level 5B", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(9), "10", "Policy Threat Level 10", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(10), "9", "Policy Threat Level 9", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(11), "4", "Policy Threat Level 4", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");

    SelenideElement rightPagination = page.paginationButtons().get(1);
    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "4", "Policy Threat Level 4B", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(1), "10", "Policy Threat Level 10", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(2), "9", "Policy Threat Level 9", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(3), "3", "Policy Threat Level 3", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(4), "3", "Policy Threat Level 3B", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(5), "10", "Policy Threat Level 10", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(6), "9", "Policy Threat Level 9", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(7), "2", "Policy Threat Level 2", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(8), "2", "Policy Threat Level 2B", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(9), "10", "Policy Threat Level 10", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(10), "9", "Policy Threat Level 9", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1B", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
  }

  @Test
  public void testDefaultSorting_MultipleViolationsSameComponent_Aggregate() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));

    testRow(RepositoryResultDetailPage.table().row(5), "10", "Policy Threat Level 10", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(6), "10", "Policy Threat Level 10", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(7), "10", "Policy Threat Level 10", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(8), "10", "Policy Threat Level 10", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(9), "10", "Policy Threat Level 10", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
  }

  @Test
  public void testThreatLevelSortingChangesDirection() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    SelenideElement rightPagination = page.paginationButtons().get(1);
    SelenideElement leftPagination = page.paginationButtons().get(0);

    // Sanity check - Threat level descending (default)
    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-10",
        "groupId10 : artifactId10 : jar : classifier10 : version10");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(11), "4", "Policy Threat Level 4", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");

    rightPagination.click();

    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "4", "Policy Threat Level 4B", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    rightPagination.click();

    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1B", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(11), "0", "No Violations", "",
        "groupId24 : artifactId24 : jar : classifier24 : version24");

    leftPagination.click();
    leftPagination.click();

    // Change Threat level ascending
    RepositoryResultDetailPage.table().header().threat().click();

    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(11), "3", "Policy Threat Level 3", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");

    rightPagination.click();

    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "4", "Policy Threat Level 4", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(11), "9", "Policy Threat Level 9", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");

    rightPagination.click();

    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "9", "Policy Threat Level 9", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(11), "0", "No Violations", "",
        "groupId24 : artifactId24 : jar : classifier24 : version24");
  }

  @Test
  public void testThreatLevelSortingChangesDirectionAggregate() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    SelenideElement rightPagination = page.paginationButtons().get(1);
    SelenideElement leftPagination = page.paginationButtons().get(0);

    // Sanity check - Threat level descending (default)
    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-10",
        "groupId10 : artifactId10 : jar : classifier10 : version10");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "",
        "groupId12 : artifactId12 : jar : classifier12 : version12");

    rightPagination.click();

    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId13 : artifactId13 : jar : classifier13 : version13");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId25 : artifactId25 : jar : classifier25 : version25");

    leftPagination.click();

    // Change Threat level ascending
    RepositoryResultDetailPage.table().header().threat().click();

    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId11 : artifactId11 : jar : classifier11 : version11");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(11), "10", "Policy Threat Level 10", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");

    rightPagination.click();

    // Check first row of the table
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");

    // Check last row of the table
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId25 : artifactId25 : jar : classifier25 : version25");
  }

  @Test
  public void testSortingAfterClickingOnColumns() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));
    RepositoryResultDetailPage.aggregateToggle().click();

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    SelenideElement leftPagination = page.paginationButtons().get(0);
    SelenideElement rightPagination = page.paginationButtons().get(1);

    // Default Ordering
    // 1: Quarantines Desc
    // 2: Threat Desc
    // 3: Policy Asc
    // 4: Component Asc
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-10",
        "groupId10 : artifactId10 : jar : classifier10 : version10");
    testRow(RepositoryResultDetailPage.table().row(11), "4", "Policy Threat Level 4", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "4", "Policy Threat Level 4B", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1B", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(11), "0", "No Violations", "",
        "groupId24 : artifactId24 : jar : classifier24 : version24");

    leftPagination.click();
    leftPagination.click();

    // Clicks on quarantined column
    RepositoryResultDetailPage.table().header().quarantined().click();

    // 1: Quarantined Asc
    // 2: Threat Desc
    // 3: Policy Asc
    // 4: Component Asc
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(11), "3", "Policy Threat Level 3", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(11), "9", "Policy Threat Level 9", "2020-06-09",
        "groupId9 : artifactId9 : jar : classifier9 : version9");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-10",
        "groupId10 : artifactId10 : jar : classifier10 : version10");
    testRow(RepositoryResultDetailPage.table().row(11), "0", "No Violations", "",
        "groupId24 : artifactId24 : jar : classifier24 : version24");

    leftPagination.click();
    leftPagination.click();

    // Clicks on Threat column
    RepositoryResultDetailPage.table().header().threat().click();

    // 1: Threat Asc
    // 2: Quarantined Asc
    // 3: Policy Asc
    // 4: Component Asc
    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(11), "3", "Policy Threat Level 3B", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "4", "Policy Threat Level 4", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(11), "9", "Policy Threat Level 9", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "9", "Policy Threat Level 9", "2020-06-09",
        "groupId9 : artifactId9 : jar : classifier9 : version9");
    testRow(RepositoryResultDetailPage.table().row(11), "0", "No Violations", "",
        "groupId24 : artifactId24 : jar : classifier24 : version24");

    leftPagination.click();
    leftPagination.click();

    // Clicks on Policy
    RepositoryResultDetailPage.table().header().policy().click();

    // 1: Policy Desc
    // 2: Threat Asc
    // 3: Quarantined Asc
    // 4: Component Asc
    testRow(RepositoryResultDetailPage.table().row(0), "9", "Policy Threat Level 9", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(11), "4", "Policy Threat Level 4B", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "4", "Policy Threat Level 4", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");
    testRow(RepositoryResultDetailPage.table().row(11), "10", "Policy Threat Level 10", "2020-06-10",
        "groupId10 : artifactId10 : jar : classifier10 : version10");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(11), "0", "No Violations", "",
        "groupId24 : artifactId24 : jar : classifier24 : version24");

    leftPagination.click();
    leftPagination.click();

    // Clicks on Component
    RepositoryResultDetailPage.table().header().component().click();

    // 1: Component Desc
    // 2: Policy Desc
    // 3: Threat Asc
    // 4: Quarantined Asc
    testRow(RepositoryResultDetailPage.table().row(0), "9", "Policy Threat Level 9", "2020-06-09",
        "groupId9 : artifactId9 : jar : classifier9 : version9");
    testRow(RepositoryResultDetailPage.table().row(11), "10", "Policy Threat Level 10", "2020-06-04",
        "groupId4 : artifactId4 : jar : classifier4 : version4");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "9", "Policy Threat Level 9", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(11), "2", "Policy Threat Level 2B", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "2", "Policy Threat Level 2", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");
    testRow(RepositoryResultDetailPage.table().row(11), "0", "Policy Threat Level 10", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    leftPagination.click();
    leftPagination.click();

    // Clicks on Threat column
    RepositoryResultDetailPage.table().header().threat().click();

    // 1: Threat Desc
    // 2: Component Desc
    // 3: Policy Desc
    // 4: Quarantined Asc
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(11), "9", "Policy Threat Level 9", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "8", "Policy Threat Level 8", "2020-06-08",
        "groupId8 : artifactId8 : jar : classifier8 : version8");
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "",
        "groupId16 : artifactId16 : jar : classifier16 : version16");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId15 : artifactId15 : jar : classifier15 : version15");
    testRow(RepositoryResultDetailPage.table().row(11), "0", "No Violations", "",
        "groupId21 : artifactId21 : jar : classifier21 : version21");
  }

  @Test
  public void testSortingAfterClickingOnColumnsAggregate() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    addQuarantinedComponentsWithPolicyViolations(repository);
    addComponentsWithoutPolicyViolations(repository);
    addNotQuarantinedComponentsWithPolicyViolations(repository);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));

    RepositoryResultDetailPage page = new RepositoryResultDetailPage();

    SelenideElement leftPagination = page.paginationButtons().get(0);
    SelenideElement rightPagination = page.paginationButtons().get(1);

    // Default Ordering
    // 1: Quarantines Desc
    // 2: Threat Desc
    // 3: Policy Asc
    // 4: Component Asc
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-10",
        "groupId10 : artifactId10 : jar : classifier10 : version10");
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "",
        "groupId12 : artifactId12 : jar : classifier12 : version12");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId13 : artifactId13 : jar : classifier13 : version13");
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId25 : artifactId25 : jar : classifier25 : version25");

    leftPagination.click();

    // Clicks on quarantined column
    RepositoryResultDetailPage.table().header().quarantined().click();

    // 1: Quarantined Asc
    // 2: Threat Desc
    // 3: Policy Asc
    // 4: Component Asc
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "",
        "groupId12 : artifactId12 : jar : classifier12 : version12");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId13 : artifactId13 : jar : classifier13 : version13");
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId25 : artifactId25 : jar : classifier25 : version25");

    leftPagination.click();

    // Clicks on Threat column
    RepositoryResultDetailPage.table().header().threat().click();

    // 1: Threat Asc
    // 2: Quarantined Asc
    // 3: Policy Asc
    // 4: Component Asc
    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId11 : artifactId11 : jar : classifier11 : version11");
    testRow(RepositoryResultDetailPage.table().row(11), "10", "Policy Threat Level 10", "2020-06-02",
        "groupId2 : artifactId2 : jar : classifier2 : version2");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-03",
        "groupId3 : artifactId3 : jar : classifier3 : version3");
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId25 : artifactId25 : jar : classifier25 : version25");

    leftPagination.click();

    // Clicks on Policy
    RepositoryResultDetailPage.table().header().policy().click();

    // 1: Policy Desc
    // 2: Threat Asc
    // 3: Quarantined Asc
    // 4: Component Asc
    testRow(RepositoryResultDetailPage.table().row(0), "9", "Policy Threat Level 9", "2020-06-09",
        "groupId9 : artifactId9 : jar : classifier9 : version9");
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "",
        "groupId12 : artifactId12 : jar : classifier12 : version12");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId13 : artifactId13 : jar : classifier13 : version13");
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId25 : artifactId25 : jar : classifier25 : version25");

    leftPagination.click();

    // Clicks on Component
    RepositoryResultDetailPage.table().header().component().click();

    // 1: Component Desc
    // 2: Policy Desc
    // 3: Threat Asc
    // 4: Quarantined Asc
    testRow(RepositoryResultDetailPage.table().row(0), "9", "Policy Threat Level 9", "2020-06-09",
        "groupId9 : artifactId9 : jar : classifier9 : version9");
    testRow(RepositoryResultDetailPage.table().row(11), "0", "No Violations", "",
        "groupId21 : artifactId21 : jar : classifier21 : version21");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "0", "No Violations", "",
        "groupId20 : artifactId20 : jar : classifier20 : version20");
    testRow(RepositoryResultDetailPage.table().row(9), "10", "Policy Threat Level 10", "2020-06-01",
        "groupId1 : artifactId1 : jar : classifier1 : version1");

    leftPagination.click();

    // Clicks on Threat column
    RepositoryResultDetailPage.table().header().threat().click();

    // 1: Threat Desc
    // 2: Component Desc
    // 3: Policy Desc
    // 4: Quarantined Asc
    testRow(RepositoryResultDetailPage.table().row(0), "10", "Policy Threat Level 10", "2020-06-05",
        "groupId5 : artifactId5 : jar : classifier5 : version5");
    testRow(RepositoryResultDetailPage.table().row(11), "1", "Policy Threat Level 1", "",
        "groupId15 : artifactId15 : jar : classifier15 : version15");

    rightPagination.click();

    testRow(RepositoryResultDetailPage.table().row(0), "1", "Policy Threat Level 1", "",
        "groupId14 : artifactId14 : jar : classifier14 : version14");
    testRow(RepositoryResultDetailPage.table().row(9), "0", "No Violations", "",
        "groupId20 : artifactId20 : jar : classifier20 : version20");
  }

  @Test
  public void testComponentDetailsPageShowsCorrectCurrentVersionWhenDifferentComponentAreOpen() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AF");
    Repository repository = tempEntity.newRepository(repositoryManager, "maven-central");

    ComponentDetails componentDetails1 =
        createComponentDetail("hash1", createComponentIdentifier("0.5.2"), "singleLicense");
    ComponentDetails componentDetails2 =
        createComponentDetail("hash2", createComponentIdentifier("0.5.3"), "singleLicense");

    List<ComponentDetails> componentDetailsArrayList = Arrays.asList(componentDetails1, componentDetails2);

    riskRemediationSetup(componentDetailsArrayList);

    createRepositoryComponent(repository, componentDetails1.getHash(), componentDetails1.getComponentIdentifier(),
        new Date(), null);
    createRepositoryComponent(repository, componentDetails2.getHash(), componentDetails2.getComponentIdentifier(),
        new Date(), null);

    refreshOrOpen(RepositoryResultDetailPage.url(repository.getId()));

    RepositoryResultTableRow row1 = RepositoryResultDetailPage.table().row(0);
    row1.click();

    waitUntilFirewallComponentDetailsPageSpinnersGone();

    RiskRemediationTile riskRemediation = firewallComponentDetailsPage.getRiskRemediationTile();
    riskRemediation.compareVersionsTitle().shouldBe(visible).shouldHave(text("Compare Versions"));
    ScrollUtil.scrollIntoView(riskRemediation.compareVersionsTitle());
    RiskRemediationTile.CompareVersionsTable table = riskRemediation.compareVersionsTable();
    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("0.5.2"));

    firewallComponentDetailsPage.backButton().click();

    RepositoryResultTableRow row2 = RepositoryResultDetailPage.table().row(1);
    row2.click();

    ScrollUtil.scrollIntoView(riskRemediation.compareVersionsTitle());
    table.versionRow().get(1).shouldHave(text("0.5.3"));
  }

  @Test
  public void testRepositoryResultPageBackButton() {
    // Firewall Dashboard
    refreshOrOpen(FirewallPage.url());
    FirewallPage page = new FirewallPage();
    getWebDriverAwait().until(ExpectedConditions.invisibilityOf(page.getAllLoadingSpinners().get(0)));
    page.firewallQuarantineTable().tableBodyRows().get(0).find("#iq-firewall-quarantine-table--repo-view-link").click();

    waitUntilUrl(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.backButton()
        .shouldBe(visible)
        .shouldHave(text("Back to Firewall Dashboard"));
    RepositoryResultDetailPage.backButton().click();

    waitUntilUrl(FirewallPage.url());
    page.firewallStatus().shouldBe(visible);

    // Repository Manager Summary View
    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    RepositoriesSummaryPage.configTile().configurationTable().repoManagerConfigTableRow(1).repoManagerConfigTableLink()
        .click();

    waitUntilUrl(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.backButton()
        .shouldBe(visible)
        .shouldHave(text(repositoryManager.getName()));
    RepositoryResultDetailPage.backButton().click();

    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text(repositoryManager.getName()));

    // Repository Managers Summary View
    refreshOrOpen(RepositoriesSummaryPage.url());
    waitUntilUrl(RepositoriesSummaryPage.url());
    RepositoriesSummaryPage.configTile().configurationTable().row(1, 2).publicId().click();

    waitUntilUrl(RepositoryResultDetailPage.url(repo.getId()));
    RepositoryResultDetailPage.backButton()
        .shouldBe(visible)
        .shouldHave(text(RepositoryContainer.SINGLETON.getName()));
    RepositoryResultDetailPage.backButton().click();

    waitUntilUrl(RepositoriesSummaryPage.url());
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text("Repository Managers"));
  }

  @Test
  public void testRepositoryResultPageFilterPopOver_ViolationsFilter() {
    Date jan1st2024 = Date.from(LocalDateTime.of(2024, 1, 1, 12, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryManager repoManager = tempEntity.newRepositoryManager("5E7PCC8D-3SAB6390-85FF543B-ECD79639-D431F7AE");
    Repository repo = tempEntity.newRepository(repoManager, "maven-central");

    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));

    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("maven-central Repository Results"));
    RepositoryResultDetailPage.table().rows().shouldHave(size(1));
    RepositoryResultDetailPage.table().rows().get(0).shouldHave(text("No results"));

    tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "path1", "hash1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), jan1st2024, jan1st2024, null);
    tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "path2", "hash2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), jan1st2024, null, null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 10, "path1", false, Action.ID_FAIL, "1", "policy1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 5, "path2", false, Action.ID_WARN, "2", "policy2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));

    refreshOrOpen(RepositoryResultDetailPage.url(repo.getId()));

    RepositoryResultDetailPage.header().shouldBe(visible).shouldHave(text("maven-central Repository Results"));
    RepositoryResultDetailPage.table().rows().shouldHave(size(2));
    testRow(RepositoryResultDetailPage.table().row(0), "10", "policy1", "2024-01-01", "g1 : a1 : v1");
    testRow(RepositoryResultDetailPage.table().row(1), "5", "policy2", "", "g2 : a2 : v2");

    RepositoryResultDetailPage.filterPopoverButton().click();
    RepositoryFilterPopover repositoryFilterPopover = RepositoryResultDetailPage.filterPopover();
    NxTreeViewMultiSelect violationsFilter = RepositoryFilterPopover.violationsFilter();

    violationsFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("4"));
    violationsFilter.multiSelectList().filter(visible).shouldBe(empty);
    violationsFilter.twisty().shouldBe(visible).click();
    violationsFilter.multiSelectList().filter(visible).shouldHave(size(5));
    violationsFilter.checkboxItem(1).shouldNotBe(selected).label().shouldHave(text("all/none"));
    violationsFilter.checkboxItem(2).shouldNotBe(selected).label().shouldHave(text("Not Violating"));
    violationsFilter.checkboxItem(3).shouldNotBe(selected).label().shouldHave(text("Open"));
    violationsFilter.checkboxItem(4).shouldNotBe(selected).label().shouldHave(text("Quarantined"));
    violationsFilter.checkboxItem(5).shouldNotBe(selected).label().shouldHave(text("Waived"));

    violationsFilter.checkboxItem(4).click();

    violationsFilter.checkboxItem(4).shouldBe(selected).label().shouldHave(text("Quarantined"));

    repositoryFilterPopover.applyButton().click();

    RepositoryResultDetailPage.table().rows().shouldHave(size(1));
    testRow(RepositoryResultDetailPage.table().row(0), "10", "policy1", "2024-01-01", "g1 : a1 : v1");

    repositoryFilterPopover.clearButton().click();

    violationsFilter.checkboxItem(1).shouldNotBe(selected).label().shouldHave(text("all/none"));
    violationsFilter.checkboxItem(2).shouldNotBe(selected).label().shouldHave(text("Not Violating"));
    violationsFilter.checkboxItem(3).shouldNotBe(selected).label().shouldHave(text("Open"));
    violationsFilter.checkboxItem(4).shouldNotBe(selected).label().shouldHave(text("Quarantined"));
    violationsFilter.checkboxItem(5).shouldNotBe(selected).label().shouldHave(text("Waived"));

    repositoryFilterPopover.applyButton().click();

    RepositoryResultDetailPage.table().rows().shouldHave(size(2));
    testRow(RepositoryResultDetailPage.table().row(0), "10", "policy1", "2024-01-01", "g1 : a1 : v1");
    testRow(RepositoryResultDetailPage.table().row(1), "5", "policy2", "", "g2 : a2 : v2");
  }

  @Test
  public void testRepositoryResultPageBackButton_hideButton() {
    String repositoryReportContainerHideBackButtonUrl = RepositoryReportContainerPage
        .url(repo.getId()) + "?hideBackButton=true";
    refreshOrOpen(repositoryReportContainerHideBackButtonUrl);

    waitUntilUrl(repositoryReportContainerHideBackButtonUrl);

    RepositoryResultDetailPage.backButton().shouldNotBe(visible);
  }

  private void riskRemediationSetup(List<ComponentDetails> componentDetailsArrayList) {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    ComponentDetails mainComponentDetail = componentDetailsArrayList.get(0);

    //addComponentDetailSecurityVulnerability(mainComponentDetail, (float) 9.1);
    //addComponentDetailSecurityVulnerability(mainComponentDetail, (float) 4.3);
    mainComponentDetail.setCatalogDate(new Date().getTime());

    componentDetailsList.setList(componentDetailsArrayList);
    ComponentDependenciesDTO componentDependenciesDTO = new ComponentDependenciesDTO(dependenciesMap, detailsMap);

    try {
      // Used when componentDetails are requested
      testCLMServer.getHdsServer().respondWith(mainComponentDetail)
          .atUri("/rest/ci/componentDetails?" + "componentIdentifier="
              + URLEncoder.encode(JsonUtils.format(mainComponentDetail.getComponentIdentifier()), "UTF-8") + "&hash="
              + mainComponentDetail.getHash());
      // Used when multi license details are requested
      testCLMServer.getHdsServer().respondWith(mainComponentDetail)
          .atUri("/rest/ci/componentDetails?" + "componentIdentifier="
              + URLEncoder.encode(JsonUtils.format(mainComponentDetail.getComponentIdentifier()), "UTF-8"));
    }
    catch (UnsupportedEncodingException e) {
      throw new UncheckedIOException(e);
    }
    testCLMServer.getHdsServer().respondWith(componentDetailsList).atUri("/rest/ci/componentDetails/list");
    testCLMServer.getHdsServer().respondWith(componentDependenciesDTO).atUri("/rest/component/dependencies");
  }

  private RepositoryComponent createRepositoryComponent(
      Repository repository,
      String hash,
      ComponentIdentifier componentIdentifier,
      Date lastEvaluationTime,
      Date quarantineTime)
  {
    String componentVersion = componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION);
    return tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/" + componentVersion + "/abi.cli-" + componentVersion + ".jar", hash,
        componentIdentifier, lastEvaluationTime, quarantineTime);
  }

  private ComponentDetails createComponentDetail(
      String hash,
      ComponentIdentifier componentIdentifier,
      String licenseCondition)
  {
    ComponentDetails componentDetails = new ComponentDetails(componentIdentifier);
    componentDetails.setHash(hash);
    componentDetails.setMatchState(MatchState.EXACT.getId());

    if (licenseCondition != "nonLicensed") {
      // default license condition is singleLicense
      componentDetails
          .setDeclaredLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("Apache-2.0"))));

      componentDetails.setObservedLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull(
          licenseCondition == "multiLicensed" ? "EPL-1.0" : "Apache-2.0"))));

      if (licenseCondition == "overriddenLicense") {
        tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, componentDetails.getComponentIdentifier(),
            LicenseOverrideStatus.OVERRIDDEN, "GPL-1.0");
      }
    }

    componentDetails.setCatalogDate(new Date().getTime());
    componentDetails.setWebsite("http://www.example.com");
    componentDetails.setLicenseThreatLevel(2);
    componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    componentDetails.setIdentificationSourceComment("No comments");
    componentDetails.setRelativePopularity(100);
    return componentDetails;
  }

  private ComponentIdentifier createComponentIdentifier(String version) {
    return ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", version, "", "jar");
  }

  private void addNotQuarantinedComponentsWithPolicyViolations(Repository repository) {
    // Add not quarantined components
    RepositoryComponent component11 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path11", "hash11", ComponentIdentifier.createMavenCoordinates("groupId11",
        "artifactId11", "version11", "classifier11", "jar"), false);

    RepositoryComponent component12 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path12", "hash12", ComponentIdentifier.createMavenCoordinates("groupId12",
        "artifactId12", "version12", "classifier12", "jar"), false);

    RepositoryComponent component13 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path13", "hash13", ComponentIdentifier.createMavenCoordinates("groupId13",
        "artifactId13", "version13", "classifier13", "jar"), false);

    RepositoryComponent component14 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path14", "hash14", ComponentIdentifier.createMavenCoordinates("groupId14",
        "artifactId14", "version14", "classifier14", "jar"), false);

    RepositoryComponent component15 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path15", "hash15", ComponentIdentifier.createMavenCoordinates("groupId15",
        "artifactId15", "version15", "classifier15", "jar"), false);

    RepositoryComponent component16 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path16", "hash16", ComponentIdentifier.createMavenCoordinates("groupId16",
        "artifactId16", "version16", "classifier16", "jar"), false);

    // Add policy violations for not quarantined components
    tempEntity.newRepositoryPolicyViolation(component11, 1, false, "Policy Threat Level 1", null);
    tempEntity.newRepositoryPolicyViolation(component12, 1, false, "Policy Threat Level 1", null);
    tempEntity.newRepositoryPolicyViolation(component13, 1, false, "Policy Threat Level 1", null);
    tempEntity.newRepositoryPolicyViolation(component14, 1, false, "Policy Threat Level 1", null);
    tempEntity.newRepositoryPolicyViolation(component15, 1, false, "Policy Threat Level 1", null);
    tempEntity.newRepositoryPolicyViolation(component16, 1, false, "Policy Threat Level 1", null);
  }

  private void addComponentsWithoutPolicyViolations(Repository repository) {
    // Add components without violations
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path20", "hash20",
        ComponentIdentifier.createMavenCoordinates("groupId20","artifactId20", "version20",
        "classifier20", "jar"), false);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path21", "hash21",
        ComponentIdentifier.createMavenCoordinates("groupId21", "artifactId21", "version21",
        "classifier21", "jar"), false);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path22", "hash22",
        ComponentIdentifier.createMavenCoordinates("groupId22", "artifactId22", "version22",
        "classifier22", "jar"), false);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path23", "hash23",
        ComponentIdentifier.createMavenCoordinates("groupId23", "artifactId23", "version23",
        "classifier23", "jar"), false);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path24", "hash24",
        ComponentIdentifier.createMavenCoordinates("groupId24", "artifactId24", "version24",
        "classifier24", "jar"), false);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path25", "hash25",
        ComponentIdentifier.createMavenCoordinates("groupId25", "artifactId25", "version25",
        "classifier25", "jar"), false);
  }

  private void addQuarantinedComponentsWithPolicyViolations(Repository repository) {
    // Add quarantined components
    Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 11, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path1", "hash1", ComponentIdentifier.createMavenCoordinates("groupId1",
        "artifactId1", "version1", "classifier1", "jar"), june1st2020, june1st2020);

    Date june2nd2020 = Date.from(LocalDateTime.of(2020, 6, 2, 10, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path2", "hash2", ComponentIdentifier.createMavenCoordinates("groupId2",
        "artifactId2", "version2", "classifier2", "jar"), june2nd2020, june2nd2020);

    Date june3rd2020 = Date.from(LocalDateTime.of(2020, 6, 3, 13, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component3 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path3", "hash3", ComponentIdentifier.createMavenCoordinates("groupId3",
        "artifactId3", "version3", "classifier3", "jar"), june3rd2020, june3rd2020);

    Date june4th2020 = Date.from(LocalDateTime.of(2020, 6, 4, 15, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component4 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path4", "hash4", ComponentIdentifier.createMavenCoordinates("groupId4",
        "artifactId4", "version4", "classifier4", "jar"), june4th2020, june4th2020);

    Date june5th2020 = Date.from(LocalDateTime.of(2020, 6, 5, 5, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component5 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path5", "hash5", ComponentIdentifier.createMavenCoordinates("groupId5",
        "artifactId5", "version5", "classifier5", "jar"), june5th2020, june5th2020);

    Date june6th2020 = Date.from(LocalDateTime.of(2020, 6, 6, 10, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component6 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path6", "hash6", ComponentIdentifier.createMavenCoordinates("groupId6",
        "artifactId6", "version6", "classifier6", "jar"), june6th2020, june6th2020);

    Date june7th2020 = Date.from(LocalDateTime.of(2020, 6, 7, 10, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component7 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path7", "hash7", ComponentIdentifier.createMavenCoordinates("groupId7",
        "artifactId7", "version7", "classifier7", "jar"), june7th2020, june7th2020);

    Date june8th2020 = Date.from(LocalDateTime.of(2020, 6, 8, 10, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component8 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path8", "hash8", ComponentIdentifier.createMavenCoordinates("groupId8",
        "artifactId8", "version8", "classifier8", "jar"), june8th2020, june8th2020);

    Date june9th2020 = Date.from(LocalDateTime.of(2020, 6, 9, 10, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component9 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path9", "hash9", ComponentIdentifier.createMavenCoordinates("groupId9",
        "artifactId9", "version9", "classifier9", "jar"), june9th2020, june9th2020);

    Date june10th2020 = Date.from(LocalDateTime.of(2020, 6, 10, 10, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    RepositoryComponent component10 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path10", "hash10", ComponentIdentifier.createMavenCoordinates("groupId10",
        "artifactId10", "version10", "classifier10", "jar"), june10th2020, june10th2020);

    // Add Policy Violations for quarantined components
    // Component 1
    tempEntity.newRepositoryPolicyViolation(component1, 1, false, "Policy Threat Level 1", null);
    tempEntity.newRepositoryPolicyViolation(component1, 1, false, "Policy Threat Level 1B", null);
    tempEntity.newRepositoryPolicyViolation(component1, 10, false, "Policy Threat Level 10", null);
    tempEntity.newRepositoryPolicyViolation(component1, 9, false, "Policy Threat Level 9", null);

    // Component 2
    tempEntity.newRepositoryPolicyViolation(component2, 2, false, "Policy Threat Level 2", null);
    tempEntity.newRepositoryPolicyViolation(component2, 2, false, "Policy Threat Level 2B", null);
    tempEntity.newRepositoryPolicyViolation(component2, 10, false, "Policy Threat Level 10", null);
    tempEntity.newRepositoryPolicyViolation(component2, 9, false, "Policy Threat Level 9", null);

    // Component 3
    tempEntity.newRepositoryPolicyViolation(component3, 3, false, "Policy Threat Level 3", null);
    tempEntity.newRepositoryPolicyViolation(component3, 3, false, "Policy Threat Level 3B", null);
    tempEntity.newRepositoryPolicyViolation(component3, 10, false, "Policy Threat Level 10", null);
    tempEntity.newRepositoryPolicyViolation(component3, 9, false, "Policy Threat Level 9", null);

    // Component 4
    tempEntity.newRepositoryPolicyViolation(component4, 4, false, "Policy Threat Level 4", null);
    tempEntity.newRepositoryPolicyViolation(component4, 4, false, "Policy Threat Level 4B", null);
    tempEntity.newRepositoryPolicyViolation(component4, 10, false, "Policy Threat Level 10", null);
    tempEntity.newRepositoryPolicyViolation(component4, 9, false, "Policy Threat Level 9", null);

    // Component 5
    tempEntity.newRepositoryPolicyViolation(component5, 5, false, "Policy Threat Level 5", null);
    tempEntity.newRepositoryPolicyViolation(component5, 5, false, "Policy Threat Level 5B", null);
    tempEntity.newRepositoryPolicyViolation(component5, 10, false, "Policy Threat Level 10", null);
    tempEntity.newRepositoryPolicyViolation(component5, 9, false, "Policy Threat Level 9", null);

    // Component 6
    tempEntity.newRepositoryPolicyViolation(component6, 6, false, "Policy Threat Level 6", null);

    // Component 7
    tempEntity.newRepositoryPolicyViolation(component7, 7, false, "Policy Threat Level 7", null);

    // Component 8
    tempEntity.newRepositoryPolicyViolation(component8, 8, false, "Policy Threat Level 8", null);

    // Component 9
    tempEntity.newRepositoryPolicyViolation(component9, 9, false, "Policy Threat Level 9", null);

    // Component 10
    tempEntity.newRepositoryPolicyViolation(component10, 10, false, "Policy Threat Level 10", null);
  }

  private void testRow(RepositoryResultTableRow row,
                       String threat,
                       String policy,
                       String quarantined,
                       String component)
  {
    row.threat().shouldHave(threat.isEmpty() ? Condition.empty : text(threat));
    row.policy().shouldHave(policy.isEmpty() ? Condition.empty : text(policy));
    row.quarantined().shouldHave(quarantined.isEmpty() ? Condition.empty : text(quarantined));
    row.component().shouldHave(component.isEmpty() ? Condition.empty : text(component));
  }
}
