/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;

import static com.sonatype.clm.testing.playwright.utils.LocatorRoleOptions.withLevel;
import static com.sonatype.clm.testing.playwright.utils.LocatorRoleOptions.withName;
import static com.sonatype.clm.testing.playwright.utils.LocatorRoleOptions.withNameExact;

/**
 * Playwright page object for the Advanced Search screen ({@code advancedSearch/AdvancedSearch.jsx}).
 */
public class AdvancedSearchPage
    extends BasePage
{
  private static final String ROOT = "#advanced-search-page";

  public AdvancedSearchPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/advancedSearch";
  }

  public static String urlWithSearchQuery(String rawQuery) {
    return "/assets/index.html#/advancedSearch?search=" + URLEncoder.encode(rawQuery, StandardCharsets.UTF_8);
  }

  public static String hashRouteWithSearchQuery(String rawQuery) {
    return "#/advancedSearch?search=" + URLEncoder.encode(rawQuery, StandardCharsets.UTF_8);
  }

  public Locator container() {
    return locator(ROOT);
  }

  /** H1 heading "Advanced Search" ({@code NxH1 id="advanced-search-page-title"}). */
  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING, withLevel(1));
  }

  public Locator queryInput() {
    return locator(ROOT + " #advanced-search-form").getByRole(AriaRole.TEXTBOX).first();
  }

  /**
   * Primary "Search" submit button ({@code NxButton id="advanced-search-button" variant="primary"}).
   * Uses role + name so the selector is resilient to ID changes. {@code setExact(true)} is required
   * because the page also contains "Add Rule" and "Add Search Terms" buttons — without exact
   * matching, {@code name="Search"} would match all three via Playwright's default substring search.
   */
  public Locator searchSubmitButton() {
    return container().getByRole(AriaRole.BUTTON, withNameExact("Search"));
  }

  /**
   * The result-count element ({@code id="advanced-search-result-count"}) rendered after a search.
   * This element has no ARIA role or accessible name — it is a plain text node whose content is
   * set dynamically at runtime (e.g. "Found 42 results"). The ID is the only stable anchor.
   */
  public Locator resultCountHeading() {
    return locator(ROOT + " #advanced-search-result-count");
  }

  public void ensureDeepLinkKeywordApplied(String keyword) {
    try {
      page.waitForFunction(
          "(kw) => { const el = document.querySelector('#advanced-search-input'); "
              + "return el != null && el.value === kw; }",
          keyword);
    }
    catch (PlaywrightException e) {
      runKeywordSearch(keyword);
    }
  }

  public void runKeywordSearch(String keyword) {
    queryInput().fill(keyword);
    searchSubmitButton().click();
  }

  public Locator useQueryBuilderButton() {
    return byRole(AriaRole.BUTTON, "Use Query Builder");
  }

  /**
   * Container element for the query builder panel ({@code id="iq-adv-search__query-builder-easy"}).
   * The panel has {@code role="region" aria-label="Advanced Search Builder"} set by the parent
   * form, so the ID selector is used here purely as a structural scope anchor — it matches the
   * one scroll-target element and does not rely on the ID for interaction.
   */
  public Locator queryBuilderContainer() {
    return locator("#iq-adv-search__query-builder-easy");
  }

  public Locator queryBuilderHeading() {
    return byRole(AriaRole.HEADING, "Build Query Rules");
  }

  public Locator addSearchItemButton() {
    return byRole(AriaRole.BUTTON, "Add Rule");
  }

  /**
   * All query-builder row containers ({@code .iq-adv-search__query-row}).
   * Each row is a structural layout div with no ARIA role or accessible name — it is not an
   * interactive element itself. The CSS class is the only stable anchor for row-level scoping;
   * all actual interactions within a row use {@code getByRole} (e.g. {@code getByRole(BUTTON)},
   * {@code getByRole(TEXTBOX)}).
   */
  public Locator queryRows() {
    return locator(".iq-adv-search__query-row");
  }

  /** Returns a {@link QueryRow} scoped to the 0-indexed row at {@code index}. */
  public QueryRow queryRow(int index) {
    return new QueryRow(queryRows().nth(index));
  }

  /** Opens the query builder panel and waits for it to be visible. */
  public void openQueryBuilder() {
    useQueryBuilderButton().click();
    queryBuilderContainer().waitFor();
  }

  public void buildSingleTermQuery(String field, String matchType, String value) {
    openQueryBuilder();
    selectFieldForRow(0, field);
    setMatchTypeForRow(0, matchType);
    setValueForRow(0, value);
  }

  /** Adds a new search item row via the "Add Rule" button. */
  public void clickAddSearchItem() {
    addSearchItemButton().click();
  }

  /** Selects a field from the field dropdown for the given row (0-indexed). */
  public void selectFieldForRow(int rowIndex, String fieldLabel) {
    queryRow(rowIndex).selectField(fieldLabel);
  }

  /** Selects match type ("Partial Match" or "Exact Match") for the given row. */
  public void setMatchTypeForRow(int rowIndex, String matchType) {
    queryRow(rowIndex).setMatchType(matchType);
  }

  /** Sets the logical operator ("AND" or "OR") for the given row (rows after the first only). */
  public void setOperatorForRow(int rowIndex, String operator) {
    queryRow(rowIndex).setOperator(operator);
  }

  /** Fills the value text input for the given row. */
  public void setValueForRow(int rowIndex, String value) {
    queryRow(rowIndex).setValue(value);
  }

  /** Removes the row at the given index via its trash icon button. */
  public void removeRow(int rowIndex) {
    queryRow(rowIndex).remove();
  }

  /** Clicks the Search button to execute the current query (query must already be populated). */
  public void clickSearchButton() {
    searchSubmitButton().click();
  }

  /** The empty-state panel shown by the query builder when all search term rows have been removed. */
  public Locator queryBuilderEmptyState() {
    return locator(".iq-adv-search__query-builder-content-empty");
  }

  /**
   * "Export Results" anchor rendered by {@code AdvancedSearchExportButton} ({@code <a role="link">}).
   * Uses accessible name so the selector is stable across ID refactors.
   */
  public Locator exportResultsButton() {
    return container().getByRole(AriaRole.LINK, withName("Export Results"));
  }

  /**
   * Clicks Export Results and waits for the browser download event.
   * Call only after a search with hits has been executed.
   */
  public Download clickExportResultsAndWaitForDownload() {
    return page.waitForDownload(() -> exportResultsButton().click());
  }

  /**
   * All result group sections rendered in the results container after a search.
   * Each group is a {@code <section aria-labelledby=...>} (an RSC {@code NxTile}), which has
   * the ARIA {@code region} landmark role when it carries an accessible name.
   */
  public Locator searchResultGroups() {
    return locator(".iq-adv-search__results-container").getByRole(AriaRole.REGION);
  }

  /**
   * The result group section whose accessible name matches {@code name}.
   * Each group section is a {@code <section aria-labelledby={headerId}>} tile; Playwright resolves
   * the accessible name from the referenced heading, so {@code name} should be the group-by value
   * (e.g. the component name used as the search term).
   */
  public Locator resultGroup(String name) {
    return locator(".iq-adv-search__results-container")
        .getByRole(AriaRole.REGION, withName(name));
  }

  /** The h2 heading of the first result group section. */
  public Locator firstResultGroupHeading() {
    return searchResultGroups().first()
        .getByRole(AriaRole.HEADING, withLevel(2));
  }

  /** An Application link inside any result card whose accessible name matches {@code applicationName}. */
  public Locator resultCardApplicationLink(String applicationName) {
    return locator(".iq-adv-search__results-container")
        .getByRole(AriaRole.LINK, withName(applicationName));
  }

  /** Clicks the Application link in the first matching result card. */
  public void clickResultCardApplicationLink(String applicationName) {
    resultCardApplicationLink(applicationName).click();
  }

  /**
   * A Report link inside any result card whose accessible name matches {@code stageName}.
   * Rendered by {@code AdvancedSearchResultCard} when a result item has a {@code policyEvaluationStage};
   * the link text equals the stage name (e.g. "build") and navigates to {@code applicationReport.policy}.
   */
  public Locator resultCardReportLink(String stageName) {
    return locator(".iq-adv-search__results-container")
        .getByRole(AriaRole.LINK, withName(stageName));
  }

  /** Clicks the Report link in the matching result card. */
  public void clickResultCardReportLink(String stageName) {
    resultCardReportLink(stageName).click();
  }

  /**
   * Clicks the first Report link for {@code stageName} found across all result cards.
   * Use when a search returns multiple result rows for the same component (e.g. one row per
   * vulnerability), so the locator resolves to more than one element and strict-mode click
   * would otherwise throw.
   */
  public void clickFirstResultCardReportLink(String stageName) {
    resultCardReportLink(stageName).first().click();
  }

  /**
   * Clicks the first Report link for {@code stageName} and waits for navigation to the
   * Application Report URL before returning. Combines the click and URL wait in one call
   * so the wait predicate is registered before the navigation event fires.
   */
  public void clickFirstResultCardReportLinkAndWaitForNavigation(String stageName) {
    PlaywrightWaitUtils.clickAndWaitForUrlContains(page, resultCardReportLink(stageName).first(),
        "/applicationReport/");
  }
}
