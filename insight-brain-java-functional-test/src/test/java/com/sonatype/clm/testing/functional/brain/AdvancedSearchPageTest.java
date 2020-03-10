/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.util.stream.IntStream;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.search.index.IndexService;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED;

public class AdvancedSearchPageTest
    extends AbstractFunctionalTest
{
  private IndexService indexService = testCLMServer.getCLMServer().getInstance(IndexService.class);

  private final AdvancedSearchPage page = new AdvancedSearchPage();

  private final SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(AdvancedSearchPage.url());
    loginAsAdmin();
  }

  @Test
  public void testOptedOut_ShowsDisabledError() {
    refreshOrOpen(AdvancedSearchPage.url());
    page.advancedSearchDisabledError().shouldBe(visible);
    page.advancedSearchEnabledContent().shouldBe(hidden);
  }

  @Test
  public void testInitialState() {
    enableAdvancedSearch();
    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().shouldBe(empty);
    page.resultCount().shouldBe(text("0"));
    page.currentPageInfo().shouldBe(text("Page 1 of 1"));
    page.errors().shouldBe(hidden);
  }

  @Test
  public void testOptedIn_ShowsEnabledContent() {
    enableAdvancedSearch();
    refreshOrOpen(AdvancedSearchPage.url());
    page.advancedSearchDisabledError().shouldBe(hidden);
    page.advancedSearchEnabledContent().shouldBe(visible);
  }

  @Test
  public void testSearch_Results_In_SinglePage() throws IOException {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("itemType:ORGANIZATION");
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    eyesWatcher.eyesCheck("Advanced Search - Single Result");

    page.resultCount().shouldBe(text("1"));
    page.currentPageInfo().shouldBe(text("Page 1 of 1"));
    page.searchInput().should(value("itemType:ORGANIZATION"));
  }

  @Test
  public void testSearch_Results_In_MultiplePages() throws IOException {
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
    page.firstResultResultNumber().shouldBe(text("1"));

    // While we are at it lets scroll to bottom to verify we can see all search results
    ScrollUtil.awaitEndOfScrolling(page.lastResultResultNumber().should(exist).scrollIntoView(true));
    page.lastResultResultNumber().shouldBe(visible).shouldBe(text("10"));

    page.nextPageButton().click();
    page.currentPageInfo().shouldBe(text("Page 2 of 2"));
    page.firstResultResultNumber().shouldBe(text("11"));

    page.previousPageButton().click();
    page.currentPageInfo().shouldBe(text("Page 1 of 2"));
    page.firstResultResultNumber().shouldBe(text("1"));
  }

  @Test
  public void testSearch_NoResults() throws IOException {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("gibberish");
    page.searchButton().click();
    FormMask.seeAndWaitForDismissal();

    page.resultCount().shouldBe(text("0"));
    page.currentPageInfo().shouldBe(text("Page 1 of 1"));

    page.nextPageButton().click();
    page.currentPageInfo().shouldBe(text("Page 1 of 1"));
  }

  @Test
  public void testNavigationAndResultRetain() throws IOException {
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
  public void testErrorsShown() throws IOException {
    enableAdvancedSearch();
    indexService.createSearchIndex();

    refreshOrOpen(AdvancedSearchPage.url());
    page.searchInput().setValue("foo:bar:baz");
    // Squeeze in verifying search can be triggered with enter button
    page.searchInput().sendKeys(Keys.ENTER);

    page.errors().shouldHave(text("The search query is invalid: Syntax Error, cannot parse foo:bar:baz:"));

    // Make sure errors are cleared upon successful search
    page.searchInput().setValue("itemType:ORGANIZATION");
    page.searchButton().click();
    page.errors().shouldBe(hidden);
  }

  private void enableAdvancedSearch() {
    dao.update(new SystemConfigurationProperty(FULL_TEXT_SEARCH_ENABLED, "true"));
  }
}
