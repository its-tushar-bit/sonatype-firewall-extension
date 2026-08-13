/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.options.WaitForSelectorState.HIDDEN;

/**
 * Extension of {@link SbomApplicationsPage} with locators added for regression test coverage.
 * Kept separate to avoid merge conflicts on the pre-existing {@link SbomApplicationsPage}.
 */
public class SbomApplicationsRegressionPage
    extends SbomApplicationsPage
{
  private static final int VIOLATIONS_COLUMN_INDEX = 5;

  public SbomApplicationsRegressionPage() {
    super();
  }

  public static String url() {
    return SbomApplicationsPage.url();
  }

  /**
   * The empty-state message cell rendered by {@code NxTableBody emptyMessage}
   * ({@code tbody .nx-cell--meta-info}) when no rows match the active filter.
   * The text reads "No applications found".
   */
  public Locator tableEmptyStateMessage() {
    return locator("#sbom-manager-applications-page tbody .nx-cell--meta-info");
  }

  /**
   * {@code NxBinaryDonutChart} for the Release Status column
   * ({@code .sbom-manager-applications-table__releaseStatusPercentageDonut}).
   * Visible when {@code application.releaseStatusPercentage} is a non-null number.
   */
  public Locator releaseStatusChart(Locator row) {
    return row.locator(".sbom-manager-applications-table__releaseStatusPercentageDonut");
  }

  /**
   * {@code NxSmallVulnerabilityCounter} for the Vulnerabilities column
   * ({@code .sbom-manager-applications-table__vulnerabilities}).
   * Visible when {@code application.vulnerabilitySummary} is non-null.
   */
  public Locator vulnerabilityCounter(Locator row) {
    return row.locator(".sbom-manager-applications-table__vulnerabilities");
  }

  /**
   * {@code NxSmallThreatCounter} for the Violations column
   * ({@code .sbom-manager-applications-table__violations}).
   * Visible when {@code application.policyViolationSummary} is non-null.
   */
  public Locator violationsCounter(Locator row) {
    return row.locator(".sbom-manager-applications-table__violations");
  }

  /**
   * Type {@code query} into the name filter character by character using
   * {@code pressSequentially}, simulating rapid keystroke entry to exercise the
   * 300 ms debounce. Waits for the loading spinner to hide before returning.
   */
  public void typeFilterByName(String query) {
    searchInput().pressSequentially(query);
    loadingSpinner().waitFor(
        new Locator.WaitForOptions().setState(HIDDEN));
  }

  /** Click the Violations column header to toggle sort state (the column is not itself sortable). */
  public void clickViolationsColumnHeader() {
    tableHeaderCells().nth(VIOLATIONS_COLUMN_INDEX).click();
  }

  /**
   * The sort button ({@code button.nx-cell__sort-btn}) inside a given header cell.
   * Present for sortable columns (Name, Latest Version, Release Status, Import Date,
   * Vulnerabilities); absent (zero matches) for non-sortable columns (Violations).
   */
  public Locator columnSortButton(Locator headerCell) {
    return headerCell.locator("button.nx-cell__sort-btn");
  }

  /**
   * The "Latest Version" {@code NxTextLink}
   * ({@code a.sbom-manager-applications-table__latest-version}) inside a table body row.
   * Clicking this link navigates to the BOM page for that SBOM version.
   */
  public Locator latestVersionLink(Locator row) {
    return row.locator("a.sbom-manager-applications-table__latest-version");
  }
}
