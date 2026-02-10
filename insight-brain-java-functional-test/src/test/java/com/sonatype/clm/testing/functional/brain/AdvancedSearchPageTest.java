/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import com.sonatype.insight.brain.search.index.HybridSearchIndexClient;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.attribute;
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

  private final SearchIndexClient searchIndexClient = testCLMServer.getCLMServer().getInstance(SearchIndexClient.class);

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

  @After
  public void tearDown() {
    cleanupIndexes();
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

  @Test
  public void testIndexNotFoundError() {
    enableAdvancedSearch();

    // MUST clean up any existing indexes from previous tests before this test runs
    // This ensures we're testing the "no index" error scenario regardless of test execution order
    cleanupIndexes();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("itemType:ORGANIZATION");
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    page.queryError().shouldBe(visible).shouldHave(text(
        "Search index not found. The Advanced Search index is unavailable or has not been created yet. " +
            "Re-indexing is required before results can be returned."));
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
    page.queryBuilderSearchTermsContainer().shouldNotBe(visible);

    // test toggle
    page.searchTermsToggleButton().click();
    page.queryBuilderSearchTermsContainer().shouldBe(visible);
    page.searchTermsToggleButton().click();
    page.queryBuilderSearchTermsContainer().shouldNotBe(visible);

    // test add prefix using pills
    page.searchTermsToggleButton().click();
    page.prefixTagWithId("organizationId").shouldNotHave(cssClass("nx-tag--selected")).click();
    // when I click on the pill it should get an additional class which fills the pill with green background
    page.prefixTagWithId("organizationId").shouldHave(cssClass("nx-tag--selected"));
    page.queryBuilderSearchTermsContainer().shouldBe(visible);  // query builder must remain open
    page.searchInput().shouldHave(value("organizationId:"));

    // test upon search query builder is closed
    page.searchInput().sendKeys("ROOT*");
    page.searchButton().click();
    page.queryBuilderSearchTermsContainer().shouldNotBe(visible);
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

  @Test
  public void testQueryBuilderToggle() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());

    // Initial state - query builder should be hidden
    page.queryBuilderEasyContainer().shouldNotBe(visible);
    page.queryBuilderToggleButton().shouldBe(visible);

    // Test toggle to show query builder
    page.queryBuilderToggleButton().click();
    page.queryBuilderEasyContainer().shouldBe(visible);
    page.searchRow(1).shouldBe(visible); // Should have one empty search item initially

    // Test toggle to hide query builder
    page.queryBuilderToggleButton().click();
    page.queryBuilderEasyContainer().shouldNotBe(visible);

    // Test that search terms builder can be toggled independently
    page.searchTermsToggleButton().click();
    page.queryBuilderEasyContainer().shouldNotBe(visible); // Should still be hidden

    // Close search terms and open query builder again
    page.searchTermsToggleButton().click();
    page.queryBuilderToggleButton().click();
    page.queryBuilderEasyContainer().shouldBe(visible);
  }

  @Test
  public void testQueryBuilderAddAndRemoveSearchItems() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.queryBuilderToggleButton().click();

    // Initially should have one empty search item (automatically added)
    page.searchRow(1).shouldBe(visible);
    page.addSearchItemButton().shouldBe(visible);
    page.queryBuilderEmptyState().shouldNotBe(visible); // Empty state not visible yet

    // Add a second search item and set a value to identify it
    page.addSearchItemButton().click();
    page.searchRowValueInput(2).setValue("second-item");
    page.searchRow(2).shouldBe(visible);

    // Add a third search item and set a value to identify it
    page.addSearchItemButton().click();
    page.searchRowValueInput(3).setValue("third-item");
    page.searchRow(3).shouldBe(visible);

    // Verify we have 3 rows total
    page.searchRow(1).shouldBe(visible);
    page.searchRow(2).shouldBe(visible);
    page.searchRow(3).shouldBe(visible);
    page.searchRow(4).shouldNotBe(visible); // No fourth row

    // Remove the second search item (index 2)
    page.searchRowRemoveButton(2).click();
    // Verify we now have 2 rows and the third item moved to position 2
    page.searchRow(1).shouldBe(visible);
    page.searchRow(2).shouldBe(visible);
    page.searchRow(3).shouldNotBe(visible);
    page.searchRowValueInput(2).shouldHave(value("third-item")); // Original third item is now at position 2

    // Remove the first search item (index 1)
    page.searchRowRemoveButton(1).click();
    // Verify we now have 1 row and the remaining item is at position 1
    page.searchRow(1).shouldBe(visible);
    page.searchRow(2).shouldNotBe(visible);
    page.searchRowValueInput(1).shouldHave(value("third-item")); // Remaining item is now at position 1

    // Remove the last search item - should show empty state
    page.searchRowRemoveButton(1).click();
    page.queryBuilderEmptyState().shouldBe(visible);
  }

  @Test
  public void testQueryBuilderFieldSelection() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.queryBuilderToggleButton().click();

    // Test field dropdown functionality
    page.searchRowFieldDropdown(1).click();
    page.fieldOption("Organization ID").shouldBe(visible);
    page.fieldOption("Application Name").shouldBe(visible);
    page.fieldOption("Component Name").shouldBe(visible);

    // Select Organization ID
    page.fieldOption("Organization ID").click();
    page.searchRowFieldDropdown(1).shouldHave(text("Organization ID"));

    // Test that value input placeholder updates
    page.searchRowValueInput(1).shouldHave(attribute("placeholder", "ROOT_ORGANIZATION_ID"));

    // Select Application Name
    page.searchRowFieldDropdown(1).click();
    page.fieldOption("Application Name").click();
    page.searchRowFieldDropdown(1).shouldHave(text("Application Name"));
    page.searchRowValueInput(1).shouldHave(attribute("placeholder", "My Application Name"));
  }

  @Test
  public void testQueryBuilderOperatorSelection() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.queryBuilderToggleButton().click();

    // Add a second search item to test operators
    page.addSearchItemButton().click();

    // First row should not have operator dropdown
    page.searchRowOperatorDropdown(1).shouldNotBe(visible);

    // Second row should have operator dropdown with default OR
    page.searchRowOperatorDropdown(2).shouldBe(visible);
    page.searchRowOperatorDropdown(2).shouldHave(text("OR"));

    // Test changing operator to AND
    page.searchRowOperatorDropdown(2).click();
    page.operatorOption("AND").click();
    page.searchRowOperatorDropdown(2).shouldHave(text("AND"));

    // Test changing back to OR
    page.searchRowOperatorDropdown(2).click();
    page.operatorOption("OR").click();
    page.searchRowOperatorDropdown(2).shouldHave(text("OR"));
  }

  @Test
  public void testQueryBuilderMatchTypeSelection() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.queryBuilderToggleButton().click();

    // Test match type dropdown
    page.searchRowMatchDropdown(1).shouldBe(visible);
    page.searchRowMatchDropdown(1).shouldHave(text("Partial Match"));

    // Change to Exact Match
    page.searchRowMatchDropdown(1).click();
    page.matchOption("Exact Match").click();
    page.searchRowMatchDropdown(1).shouldHave(text("Exact Match"));

    // Change back to Partial Match
    page.searchRowMatchDropdown(1).click();
    page.matchOption("Partial Match").click();
    page.searchRowMatchDropdown(1).shouldHave(text("Partial Match"));
  }

  @Test
  public void testQueryBuilderValueInput() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.queryBuilderToggleButton().click();

    // Test value input functionality
    page.searchRowValueInput(1).shouldBe(visible);
    page.searchRowValueInput(1).setValue("test-value");
    page.searchRowValueInput(1).shouldHave(value("test-value"));

    // Test that value persists when changing other fields
    page.searchRowFieldDropdown(1).click();
    page.fieldOption("Application Name").click();
    page.searchRowValueInput(1).shouldHave(value("test-value"));

    // Test clearing value
    page.searchRowValueInput(1).clear();
    page.searchRowValueInput(1).shouldBe(empty);
  }

  @Test
  public void testQueryBuilderSearchExecution() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.queryBuilderToggleButton().click();

    // Set up a simple query
    page.searchRowFieldDropdown(1).click();
    page.fieldOption("Item Type").click();
    page.searchRowValueInput(1).setValue("ORGANIZATION");

    // Execute search
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    // Verify search was executed and query builder is closed
    page.queryBuilderEasyContainer().shouldNotBe(visible);
    page.searchInput().shouldHave(value("itemType:*ORGANIZATION*"));
    page.resultCount().shouldBe(text("1"));
  }

  @Test
  public void testQueryBuilderComplexQuery() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.queryBuilderToggleButton().click();

    // Set up first search item
    page.searchRowFieldDropdown(1).click();
    page.fieldOption("Item Type").click();
    page.searchRowValueInput(1).setValue("ORGANIZATION");

    // Add second search item
    page.addSearchItemButton().click();
    page.searchRowFieldDropdown(2).click();
    page.fieldOption("Item Type").click();
    page.searchRowValueInput(2).setValue("APPLICATION");

    // Change operator to AND
    page.searchRowOperatorDropdown(2).click();
    page.operatorOption("AND").click();

    // Add third search item
    page.addSearchItemButton().click();
    page.searchRowFieldDropdown(3).click();
    page.fieldOption("Component Name").click();
    page.searchRowValueInput(3).setValue("test-component");

    // Change operator to OR
    page.searchRowOperatorDropdown(3).click();
    page.operatorOption("OR").click();

    // Execute search
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    // Verify complex query was built correctly
    page.searchInput().shouldHave(
        value("itemType:*ORGANIZATION* AND itemType:*APPLICATION* OR componentName:*test-component*"));
  }

  @Test
  public void testQueryBuilderWithExactMatch() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.queryBuilderToggleButton().click();

    // Set up query with exact match
    page.searchRowFieldDropdown(1).click();
    page.fieldOption("Item Type").click();
    page.searchRowMatchDropdown(1).click();
    page.matchOption("Exact Match").click();
    page.searchRowValueInput(1).setValue("ORGANIZATION");

    // Execute search
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    // Verify exact match query (no wildcard)
    page.searchInput().shouldHave(value("itemType:\"ORGANIZATION\""));
  }

  @Test
  public void testQueryBuilderIntegrationWithSearchTerms() {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());

    // Start with search terms builder
    page.searchTermsToggleButton().click();
    page.searchInput().setValue("itemType:ORGANIZATION");
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    // Switch to query builder
    page.queryBuilderToggleButton().click();
    page.queryBuilderEasyContainer().shouldBe(visible);

    // Add a new search item in query builder
    page.searchRowFieldDropdown(1).click();
    page.fieldOption("Item Type").click();
    page.searchRowValueInput(1).setValue("APPLICATION");

    // Execute search from query builder
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    // Verify query was updated
    page.searchInput().shouldHave(value("itemType:*APPLICATION*"));
  }

  private void cleanupIndexes() {
    if (searchIndexClient instanceof OpenSearchSearchIndexClient openSearchClient) {
      cleanupOpenSearchIndex(openSearchClient);
    }
    else if (searchIndexClient instanceof HybridSearchIndexClient hybridClient) {
      cleanupHybridIndex(hybridClient);
    }
    else if (searchIndexClient instanceof LuceneSearchIndexClient) {
      cleanupLuceneIndex();
    }
  }

  private void cleanupOpenSearchIndex(OpenSearchSearchIndexClient openSearchClient) {
    try {
      openSearchClient.deleteIndex();
    }
    catch (Exception e) {
      // Ignore errors if index doesn't exist
    }
  }

  private void cleanupHybridIndex(HybridSearchIndexClient hybridClient) {
    if (hybridClient.getPrimaryClient() instanceof OpenSearchSearchIndexClient primaryClient) {
      cleanupOpenSearchIndex(primaryClient);
    }
    cleanupLuceneIndex();
  }

  private void cleanupLuceneIndex() {
    try {
      deleteLuceneIndexDirectory();
    }
    catch (Exception e) {
      // Ignore errors if directory doesn't exist
    }
  }

  private void deleteLuceneIndexDirectory() throws Exception {
    Path indexDir = insightWork.getSearchIndexDir().toPath();
    if (Files.exists(indexDir)) {
      try (Stream<Path> walk = Files.walk(indexDir)) {
        walk.sorted(Comparator.reverseOrder())
            .forEach(path -> {
              try {
                Files.delete(path);
              }
              catch (Exception e) {
                // Ignore errors during cleanup
              }
            });
      }
    }
  }
}
