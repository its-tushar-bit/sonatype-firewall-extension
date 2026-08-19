/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.utils.PlaywrightUrlAssertions;

import com.microsoft.playwright.Download;
import org.assertj.core.api.Assertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link AdvancedSearchPage}.
 */
public class AdvancedSearchPageAssertions
{
  private final AdvancedSearchPage page;

  public AdvancedSearchPageAssertions(AdvancedSearchPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
  }

  public void shouldHaveHeading(String expected) {
    assertThat(page.pageHeading()).hasText(expected);
  }

  public void shouldHaveQueryValue(String expected) {
    assertThat(page.queryInput()).hasValue(expected);
  }

  /** Asserts the query builder panel is visible with its "Build Query Rules" heading. */
  public void shouldShowQueryBuilderLoaded() {
    assertThat(page.queryBuilderContainer()).isVisible();
    assertThat(page.queryBuilderHeading()).isVisible();
    assertThat(page.addSearchItemButton()).isVisible();
  }

  /** Asserts the exact number of search term rows currently shown in the builder. */
  public void shouldHaveQueryRowCount(int expectedCount) {
    assertThat(page.queryRows()).hasCount(expectedCount);
  }

  /**
   * Asserts the field dropdown for the given row shows {@code expectedFieldLabel}
   * (i.e. a field has been selected).
   */
  public void shouldHaveFieldSelectedForRow(int rowIndex, String expectedFieldLabel) {
    assertThat(page.queryRow(rowIndex).fieldButton()).hasText(expectedFieldLabel);
  }

  /**
   * Asserts the operator dropdown for the given row (must be row index >= 1) shows
   * {@code expectedOperator}.
   */
  public void shouldHaveOperatorForRow(int rowIndex, String expectedOperator) {
    assertThat(page.queryRow(rowIndex).operatorButton()).hasText(expectedOperator);
  }

  /**
   * Asserts the main search query input contains {@code expectedSubstring} as a literal substring.
   *
   * <p>
   * {@code PlaywrightAssertions.assertThat(locator).hasValue(String)} performs an exact match, so
   * a {@link Pattern} is required for partial-match behaviour. Metacharacters in
   * {@code expectedSubstring} are escaped manually rather than via {@code Pattern.quote()}: the
   * {@code \Q...\E} quoting syntax produced by {@code Pattern.quote()} is a Java-only extension
   * that is not recognised by the JavaScript regex engine Playwright uses internally.
   */
  public void shouldHaveQueryContaining(String expectedSubstring) {
    String escaped = expectedSubstring.replaceAll("([\\\\^$.|?*+()\\[\\]{}])", "\\\\$1");
    assertThat(page.queryInput()).hasValue(Pattern.compile(".*" + escaped + ".*"));
  }

  /**
   * Asserts the main search query input value matches {@code pattern} exactly.
   */
  public void shouldHaveQueryMatching(Pattern pattern) {
    assertThat(page.queryInput()).hasValue(pattern);
  }

  /** Asserts the result count heading is visible, indicating a search has been executed. */
  public void shouldShowSearchResultCount() {
    assertThat(page.resultCountHeading()).isVisible();
  }

  /** Asserts the result-count heading reads "Results: {expected}". */
  public void shouldHaveResultCount(int expected) {
    assertThat(page.resultCountHeading()).containsText("Results: " + expected);
  }

  public void shouldHaveNoResults() {
    shouldHaveResultCount(0);
  }

  public void shouldHaveAtLeastOneResult() {
    assertThat(page.resultCountHeading()).containsText(Pattern.compile("Results: [1-9]"));
  }

  /** Asserts the "Add Rule" button is visible in the query builder. */
  public void shouldHaveAddSearchItemVisible() {
    assertThat(page.addSearchItemButton()).isVisible();
  }

  /** Asserts the query input has an empty value, indicating all builder rows have been removed. */
  public void shouldHaveEmptyQuery() {
    assertThat(page.queryInput()).hasValue("");
  }

  /** Asserts the Search button is disabled (no query to submit). */
  public void shouldHaveSearchButtonDisabled() {
    assertThat(page.searchSubmitButton()).isDisabled();
  }

  /** Asserts the query builder shows its empty state after all search term rows are removed. */
  public void shouldHaveQueryBuilderEmptyState() {
    assertThat(page.queryBuilderEmptyState()).isVisible();
  }

  /**
   * Asserts the Export Results button is enabled (i.e. a search with hits has been executed).
   * The button is an {@code <a>} element; disabled state is conveyed via {@code aria-disabled="true"}.
   */
  public void shouldHaveExportButtonEnabled() {
    assertThat(page.exportResultsButton()).not().hasAttribute("aria-disabled", "true");
  }

  /** Asserts exactly one result group section is shown after a search. */
  public void shouldHaveExactlyOneResultGroup() {
    assertThat(page.searchResultGroups()).hasCount(1);
  }

  /** Asserts the first result group section heading contains {@code text}. */
  public void shouldHaveFirstResultGroupHeadingContaining(String text) {
    assertThat(page.firstResultGroupHeading()).containsText(text);
  }

  /** Asserts a result group section with accessible name {@code groupName} is visible. */
  public void shouldHaveResultGroup(String groupName) {
    assertThat(page.resultGroup(groupName)).isVisible();
  }

  /** Asserts the Application link for {@code applicationName} is visible in a result card. */
  public void shouldHaveApplicationLinkVisible(String applicationName) {
    assertThat(page.resultCardApplicationLink(applicationName)).isVisible();
  }

  /** Asserts the Report link for {@code stageName} is visible in a result card. */
  public void shouldHaveReportLinkVisible(String stageName) {
    assertThat(page.resultCardReportLink(stageName)).isVisible();
  }

  /**
   * Asserts the main search query input does NOT contain {@code forbiddenSubstring}.
   */
  public void shouldNotHaveQueryContaining(String forbiddenSubstring) {
    String escaped = forbiddenSubstring.replaceAll("([\\\\^$.|?*+()\\[\\]{}])", "\\\\$1");
    assertThat(page.queryInput()).not().hasValue(Pattern.compile(".*" + escaped + ".*"));
  }

  /**
   * Asserts the query input does not wrap {@code value} in wildcards ({@code *value*}),
   * which verifies an Exact Match term was generated instead of a Partial Match term.
   */
  public void shouldNotHaveWildcardWrapForValue(String value) {
    String escaped = value.replaceAll("([\\\\^$.|?*+()\\[\\]{}])", "\\\\$1");
    assertThat(page.queryInput()).not().hasValue(Pattern.compile(".*\\*" + escaped + "\\*.*"));
  }

  /**
   * Asserts a downloaded file name looks like a CSV export. Playwright may derive the suggested
   * name from the URL path when {@code Content-Disposition} is not honoured by {@code route.fulfill()}.
   */
  public void shouldHaveCsvDownload(Download download) {
    Assertions.assertThat(download.suggestedFilename().toLowerCase())
        .as("Expected CSV download but got: " + download.suggestedFilename())
        .contains("csv");
  }

  /** Asserts navigation landed on a URL containing {@code urlFragment} (SPA hash routes). */
  public void shouldHaveUrlContaining(String urlFragment) {
    PlaywrightUrlAssertions.assertUrlContaining(page.playwrightPage(), urlFragment);
  }
}
