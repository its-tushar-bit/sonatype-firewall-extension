/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link ReportListPage}. */
public class ReportListPageAssertions
{
  private final ReportListPage page;

  public ReportListPageAssertions(ReportListPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowPageTitle(String expectedTitle) {
    assertThat(page.pageHeading()).hasText(expectedTitle);
  }

  public void shouldShowFilterInput() {
    assertThat(page.filterInput()).isVisible();
  }

  public void shouldHaveRowCount(int expectedCount) {
    assertThat(page.rows()).hasCount(expectedCount);
  }

  public void shouldHaveRowCountAtLeast(int minimum) {
    assertThat(page.rows().nth(minimum - 1)).isVisible();
  }

  public void shouldShowEmptyMessage() {
    assertThat(page.emptyMessage()).isVisible();
  }

  public void shouldHaveAriaSort(Locator headerCell, String expectedAriaSort) {
    assertThat(headerCell).hasAttribute("aria-sort", expectedAriaSort);
  }

  /** Asserts the header cell has no {@code aria-sort} attribute at all. */
  public void shouldNotBeSortable(Locator headerCell) {
    assertThat(headerCell).not().hasAttribute("aria-sort", ANY_VALUE);
  }

  private static final Pattern ANY_VALUE = Pattern.compile(".*");

  public void shouldShowNoViolationsText(Locator stageCell) {
    assertThat(page.noViolationsTextIn(stageCell)).isVisible();
  }

  public void shouldShowBothReportAndPrioritiesLinks(Locator row) {
    assertThat(page.buildReportLinkOf(row)).isVisible();
    assertThat(page.buildLifecyclePrioritiesLinkOf(row)).isVisible();
  }

  public void shouldShowOnlyDeveloperPrioritiesLink(Locator row) {
    assertThat(page.buildDeveloperOnlyPrioritiesLinkOf(row)).isVisible();
    assertThat(page.buildReportLinkOf(row)).hasCount(0);
  }

  public void shouldShowSourcePendingState(Locator row) {
    assertThat(page.sourcePendingTextIn(page.sourceCellOf(row))).isVisible();
  }

  public void shouldShowShowContactButton(Locator row) {
    assertThat(page.showContactButtonOf(row)).isVisible();
  }

  /** Anchors on the cell's text rather than the role-less name div. */
  public void shouldShowLoadedContactName(Locator row, String expectedDisplayName) {
    assertThat(page.appCellOf(row)).containsText(expectedDisplayName);
  }

  public void shouldShowContactLoadingSpinner(Locator row) {
    assertThat(page.contactLoadingSpinnerOf(row)).isVisible();
  }

  public void shouldShowContactErrorState(Locator row) {
    assertThat(page.contactErrorTextOf(row)).isVisible();
    assertThat(page.contactErrorTextOf(row)).hasText("Error loading contact");
  }

  public void shouldShowLoadMoreButton() {
    assertThat(page.loadButton()).isVisible();
  }
}
