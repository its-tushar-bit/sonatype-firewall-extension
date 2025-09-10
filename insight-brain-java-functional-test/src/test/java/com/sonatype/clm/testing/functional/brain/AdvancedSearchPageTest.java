/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.stream.IntStream;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;

public class AdvancedSearchPageTest
    extends AbstractFunctionalTest
{
  private final IndexService indexService = testCLMServer.getCLMServer().getInstance(IndexService.class);

  private final AdvancedSearchPage page = new AdvancedSearchPage();

  private final InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  private SystemConfigurationPropertyDAO dao;

  private PolicyEvaluation newAppReport(String appId, String stageId, String reportId, String reportResourceName)
      throws Exception
  {
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(appId, stageId, reportId);
    ReportTestUtils.createReportFile(policyEval.getApplicationId(), policyEval.getScanId(),
        ReportTestUtils.zipReportDir(reportResourceName, tempDir), insightWork);
    return policyEval;
  }

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(AdvancedSearchPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    dao = lookup(SystemConfigurationPropertyDAO.class);
  }

  @Test
  public void testOptedOut_ShowsDisabledError() {
    refreshOrOpen(AdvancedSearchPage.url());
    page.advancedSearchDisabledError().shouldBe(visible);
    page.advancedSearchPageTitle().shouldBe(hidden);
  }

  @Test
  public void testInitialState() {
    enableAdvancedSearch();
    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().shouldBe(empty);
    page.resultCount().shouldBe(text("0"));
    page.currentPageInfo().shouldBe(hidden);
    page.advancedSearchDisabledError().shouldBe(hidden);
  }

  @Test
  public void testOptedIn_ShowsEnabledContent() {
    enableAdvancedSearch();
    refreshOrOpen(AdvancedSearchPage.url());
    page.advancedSearchDisabledError().shouldBe(hidden);
    page.advancedSearchPageTitle().shouldBe(visible);
  }

  @Test
  public void testSearch_Results_In_SinglePage() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("itemType:ORGANIZATION");
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    page.resultCount().shouldBe(text("1"));
    page.currentPageInfo().shouldBe(text("Page 1 of 1"));
    page.searchInput().should(value("itemType:ORGANIZATION"));
  }

  @Test
  public void testSearch_Results_In_MultiplePages() {
    enableAdvancedSearch();

    // Create 15 policies
    IntStream.range(0, 15).forEach(i -> tempEntity.newPolicy());
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("itemType:POLICY");
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    page.resultCount().shouldBe(text("15"));
    page.currentPageInfo().shouldBe(text("Page 1 of 2"));

    page.nextPageButton().click();
    page.currentPageInfo().shouldBe(text("Page 2 of 2"));

    page.previousPageButton().click();
    page.currentPageInfo().shouldBe(text("Page 1 of 2"));
  }

  @Test
  public void testSearch_NoResults() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("gibberish");
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    page.resultCount().shouldBe(text("0"));
    page.currentPageInfo().shouldBe(hidden);
    page.nextPageButton().shouldBe(disabled);
  }

  @Test
  public void testNavigationAndResultRetain() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("itemType:ORGANIZATION");
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    page.firstSearchResultLink().click();
    waitUntilUrl(OwnerSummaryPage.urlToRootOrg());

    // Go back to Advanced Search Page and verify our search and results are retained
    refreshOrOpen(AdvancedSearchPage.url());
    page.resultCount().shouldBe(text("1"));
    page.currentPageInfo().shouldBe(text("Page 1 of 1"));
    page.searchInput().should(value("itemType:ORGANIZATION"));
  }

  @Test
  public void testErrorsShown() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("foo:bar:baz");
    // Squeeze in verifying search can be triggered with enter button
    page.searchInput().sendKeys(Keys.ENTER);

    page.queryError().shouldHave(text(
        "The search query is invalid: Syntax Error, cannot parse foo:bar:baz -itemType:NON_VULNERABLE_COMPONENT:"));

    // Make sure errors are cleared upon successful search
    page.searchInput().setValue("itemType:ORGANIZATION");
    page.searchButton().click();
    page.queryError().shouldBe(hidden);
  }

  private void enableAdvancedSearch() {
    dao.update(new SystemConfigurationProperty(ADVANCED_SEARCH_ENABLED, "true"));
  }

  @Test
  public void testQueryResetWhenNextAndPrevious() {
    // When a user searches with foo:bar and there are multiple pages in the result
    // If the user modifies the search bar and clicks either next or previous
    // we must set the input field back to foo:bar and navigate in pages for this search

    // Create 15 policies
    IntStream.range(0, 15).forEach(i -> tempEntity.newPolicy());

    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("itemType:POLICY");
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    page.resultCount().shouldBe(text("15"));
    page.currentPageInfo().shouldBe(text("Page 1 of 2"));

    page.searchInput().clear();

    page.nextPageButton().click();
    page.currentPageInfo().shouldBe(text("Page 2 of 2"));
    page.searchInput().shouldBe(value("itemType:POLICY"));

    page.searchInput().setValue("itemType:ORGANIZATION");

    page.previousPageButton().click();
    page.searchInput().shouldBe(value("itemType:POLICY"));
    page.currentPageInfo().shouldBe(text("Page 1 of 2"));
  }

  @Test
  public void testHelpContainer() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());

    // Test initial state
    page.helpContainerToggle().shouldBe(visible);
    page.helpContainer().shouldNotBe(visible);

    // Test toggle
    page.helpContainerToggle().click();
    page.helpContainer().shouldBe(visible);
    page.helpContainerToggle().click();
    page.helpContainer().shouldNotBe(visible);

    // Test I can leave help open and it remains open when I come back
    page.helpContainerToggle().click();
    page.helpContainerToggle().shouldBe(visible);

    refreshOrOpen(DashboardPage.url());
    refreshOrOpen(AdvancedSearchPage.url());

    page.helpContainer().shouldBe(visible);
  }

  @Test
  public void testQueryBuilder() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());

    // initial state
    page.queryBuilderContainer().shouldNotBe(visible);

    // test toggle
    page.queryBuilderButton().click();
    page.queryBuilderContainer().shouldBe(visible);
    page.queryBuilderButton().click();
    page.queryBuilderContainer().shouldNotBe(visible);

    // test add prefix using pills
    page.queryBuilderButton().click();
    page.prefixTagWithId("organizationId").shouldNotHave(cssClass("nx-tag--selected")).click();
    // when I click on the pill it should get an additional class which fills the pill with green background
    page.prefixTagWithId("organizationId").shouldHave(cssClass("nx-tag--selected"));
    page.queryBuilderContainer().shouldBe(visible);  // query builder must remain open
    page.searchInput().shouldHave(value("organizationId:"));

    // test upon search query builder is closed
    page.searchInput().sendKeys("ROOT*");
    page.searchButton().click();
    page.queryBuilderContainer().shouldNotBe(visible);
  }

  @Test
  public void testSearch_Include_All_Components() throws Exception {
    Organization organization = tempEntity.newOrganization("my-org");
    Application application = tempEntity.newApplication("my-app", "my-app", organization.getId());
    newAppReport(application.getId(), Stage.ID_RELEASE, "report-id", "/IndexSearchingTest/nonVulnerableComponents");
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());

    // Radio buttons should not be visible on initial load
    page.componentSearchRadioButtons().shouldNotBe(visible);

    // Radio buttons should be visible, default search should return
    // components with vulnerability information
    page.searchInput().setValue("componentName:*artifact*");
    page.componentSearchRadioButtons().shouldBe(visible);
    page.showAllComponentsRadio().shouldBe(visible);

    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    // Default results should exist (components with vulnerabilities only)
    // some assertions
    page.resultCount().shouldBe(text("1"));
    page.firstResultCardOrgName().shouldBe(text(organization.getName()));
    page.firstResultCardAppName().shouldBe(text(application.getName()));

    // Rerun search with show all components selected
    page.showAllComponentsRadio().click();
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    // There should be more results (vulnerable and non-vulnerable)
    page.resultCount().shouldBe(text("2"));
    page.firstResultCardOrgName().shouldBe(text(organization.getName()));
    page.firstResultCardAppName().shouldBe(text(application.getName()));
    page.secondResultCardOrgName().shouldBe(text(organization.getName()));
    page.secondResultCardAppName().shouldBe(text(application.getName()));

    eyesWatcher.eyesCheck("Show all components radio buttons");
  }
}
