/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.options.WaitForSelectorState.HIDDEN;

/**
 * Playwright page object for the SBOM Applications page.
 */
public class SbomApplicationsPage
    extends BasePage
{
  private static final String ROOT = "#sbom-manager-applications-page";

  public SbomApplicationsPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/sbomManager/applications";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator title() {
    return locator(ROOT + " .nx-h1");
  }

  /** All table rows (including header). Kept for backward compatibility. */
  public Locator applicationRows() {
    return locator(ROOT + " .nx-table-row");
  }

  /** Body rows only (excludes the header row). */
  public Locator tableBodyRows() {
    return locator(ROOT + " tbody .nx-table-row:not(.nx-table-row--header)");
  }

  public Locator tableHeaderCells() {
    return locator(ROOT + " .nx-table-row--header:not(.nx-table-row--filter-header) .nx-cell--header");
  }

  /**
   * The NxFilterInput search field. NxTextInput places {@code id} on the {@code <input>} element
   * itself, so {@code #application-name-filter} resolves directly to the input — no child selector needed.
   */
  public Locator searchInput() {
    return locator(ROOT + " #application-name-filter");
  }

  public Locator loadingSpinner() {
    return locator(ROOT + " .nx-loading-spinner");
  }

  public Locator paginationStatus() {
    return locator(ROOT + " .sbom-manager-applications-table__pagination-status");
  }

  /**
   * Sort button inside the Name column header.
   * NxTable sortable cells render a {@code <button>} inside {@code
   *
  <th>}; clicking the button
   * is the reliable target for triggering a sort.
   */
  public Locator nameColumnHeader() {
    return locator(ROOT
        + " #sbom-manager-applications-table thead tr:not(.nx-table-row--filter-header) th.nx-cell--header:first-child button.nx-cell__sort-btn");
  }

  /**
   * Page button in the NxPagination component by its aria-label.
   * <p>
   * NxPagination aria-label rules:
   * <ul>
   * <li>First page button → {@code "goto first page"}</li>
   * <li>Last page button → {@code "goto last page"}</li>
   * <li>Middle page buttons → {@code "goto page N"} (1-based)</li>
   * <li>Previous/Next arrows → {@code "goto previous page"} / {@code "goto next page"}</li>
   * </ul>
   */
  public Locator paginationButtonByLabel(String ariaLabel) {
    return locator(ROOT + " nav.nx-btn-bar--pagination")
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(ariaLabel).setExact(true));
  }

  // --------------- Actions ---------------

  /**
   * Fill the NxFilterInput search field with {@code query} and wait for the table body to
   * reflect the filtered results (spinner hidden / DOM settled).
   */
  public void filterByName(String query) {
    searchInput().fill(query);
    loadingSpinner().waitFor(
        new Locator.WaitForOptions().setState(HIDDEN));
  }

  /**
   * Clear the search filter by emptying the input, equivalent to typing nothing.
   */
  public void clearFilter() {
    searchInput().fill("");
    loadingSpinner().waitFor(
        new Locator.WaitForOptions().setState(HIDDEN));
  }

  /**
   * Click the Name column sort button. The first click sorts ascending; the second descending.
   */
  public void clickNameColumnSort() {
    nameColumnHeader().click();
    loadingSpinner().waitFor(
        new Locator.WaitForOptions().setState(HIDDEN));
  }
}
