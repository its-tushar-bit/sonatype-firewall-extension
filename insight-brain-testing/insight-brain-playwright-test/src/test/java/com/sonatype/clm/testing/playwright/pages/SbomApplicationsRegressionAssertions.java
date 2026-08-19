/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link SbomApplicationsRegressionPage}. */
public class SbomApplicationsRegressionAssertions
    extends SbomApplicationsPageAssertions
{
  private static final int SORTABLE_COLUMN_COUNT = 5;

  private static final int NAME_COLUMN_INDEX = 0;

  // "vulnerabilities" lowercase: the NxSmallVulnerabilityCounter column renders no capitalised header text.
  private static final String[] COLUMN_NAMES =
      {"Name", "Latest Version", "Release Status", "Import Date", "vulnerabilities", "Violations"};

  private static final String RELATIVE_DATE_TEXT = "ago";

  private static final String EMPTY_STATE_MESSAGE = "No applications found";

  private final SbomApplicationsRegressionPage regressionPage;

  public SbomApplicationsRegressionAssertions(SbomApplicationsRegressionPage page) {
    super(page);
    this.regressionPage = page;
  }

  public void shouldShowSingleMatchingRow(String text) {
    shouldHaveRowCount(1);
    firstRowShouldContainText(text);
  }

  public void firstRowCellsShouldShowCountersAndRelativeDate() {
    Locator firstRow = regressionPage.tableBodyRows().first();
    assertThat(regressionPage.releaseStatusChart(firstRow)).isVisible();
    assertThat(regressionPage.vulnerabilityCounter(firstRow)).isVisible();
    assertThat(regressionPage.violationsCounter(firstRow)).isHidden();
    assertThat(firstRow).containsText(RELATIVE_DATE_TEXT);
  }

  public void firstRowCellsShouldShowViolationsCounter() {
    Locator firstRow = regressionPage.tableBodyRows().first();
    assertThat(regressionPage.violationsCounter(firstRow)).isVisible();
  }

  public void shouldShowMultiPagePagination(int showing, int total) {
    shouldShowPaginationStatus("Showing " + showing + " of " + total + " applications");
    assertThat(regressionPage.paginationButtonByLabel("goto next page")).isVisible();
  }

  public void shouldShowSingleMatchingRowWithViolations(String text) {
    shouldShowSingleMatchingRow(text);
    firstRowCellsShouldShowViolationsCounter();
  }

  public void shouldShowEmptyStateMessage() {
    assertThat(regressionPage.tableEmptyStateMessage()).containsText(EMPTY_STATE_MESSAGE);
  }

  public void nameColumnSortButtonShouldBeVisible() {
    Locator nameHeader = regressionPage.tableHeaderCells().nth(NAME_COLUMN_INDEX);
    assertThat(regressionPage.columnSortButton(nameHeader)).isVisible();
  }

  public void nameColumnSortButtonShouldBeHidden() {
    Locator nameHeader = regressionPage.tableHeaderCells().nth(NAME_COLUMN_INDEX);
    assertThat(regressionPage.columnSortButton(nameHeader)).isHidden();
  }

  public void shouldHaveExpectedColumnsWithSortability() {
    for (int i = 0; i < COLUMN_NAMES.length; i++) {
      Locator header = regressionPage.tableHeaderCells().nth(i);
      assertThat(header).containsText(COLUMN_NAMES[i]);
      if (i < SORTABLE_COLUMN_COUNT) {
        assertThat(regressionPage.columnSortButton(header)).isVisible();
      }
      else {
        assertThat(regressionPage.columnSortButton(header)).isHidden();
      }
    }
  }
}
