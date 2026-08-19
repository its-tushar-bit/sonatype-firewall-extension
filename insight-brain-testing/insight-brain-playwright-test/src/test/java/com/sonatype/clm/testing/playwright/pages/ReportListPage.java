/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Report List page (Reports / violations). Columns: Application | Organization | Source | Build | Stage Release |
 * Release.
 */
public class ReportListPage
    extends BasePage
{
  public static final String LIFECYCLE_TITLE = "Reports";

  public static final String DEVELOPER_TITLE = "Priorities";

  private static final String EMPTY_TABLE_MESSAGE = "No data found.";

  private static final String NO_VIOLATIONS_TEXT = "No violations";

  private static final String PENDING_TEXT = "pending";

  private static final String LOAD_MORE_BUTTON_NAME = "Load More Results";

  private static final String SHOW_CONTACT_BUTTON_NAME = "Show Contact";

  private static final String FILTER_PLACEHOLDER = "Search by application or organization name";

  private static final Pattern REPORT_LINK_NAME_PATTERN = Pattern.compile("^(View )?Report$");

  private static final String LIFECYCLE_PRIORITIES_LINK_NAME = "Priorities";

  private static final String DEVELOPER_PRIORITIES_LINK_NAME = "View Priorities";

  private static final String SOURCE_COLUMN_NAME = "Source";

  private static final String BUILD_COLUMN_NAME = "Build";

  private static final String STAGE_RELEASE_COLUMN_NAME = "Stage Release";

  private static final String RELEASE_COLUMN_NAME = "Release";

  public ReportListPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/reports/violations";
  }

  public static String developerPrioritiesUrl() {
    return "/assets/index.html#/developer/priorities";
  }

  private static final String ROOT_ID = "iq-report-container";

  private static final String TABLE_ID = "iq-violation-table";

  public Locator container() {
    return locator("#" + ROOT_ID);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(1));
  }

  public Locator filterInput() {
    return container().getByPlaceholder(FILTER_PLACEHOLDER);
  }

  public Locator loadButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(LOAD_MORE_BUTTON_NAME));
  }

  public Locator table() {
    return locator("#" + TABLE_ID);
  }

  /**
   * Data rows only. Anchored on the {@code iq-violation-table-row} class
   * (ReportsPage.jsx:147) so the empty-state and Load-More tbody siblings are excluded
   * structurally — no text exclusion needed (and {@code FilterOptions.setHasNotText} only
   * keeps the last call's value, which makes a chain of two silently drop one).
   */
  public Locator rows() {
    return table().locator("tbody > tr.iq-violation-table-row");
  }

  public Locator firstRow() {
    return rows().first();
  }

  /**
   * Prefer over {@link #firstRow()} when the test needs a specific app — {@code firstRow()}
   * after a fresh {@link #typeFilter} can briefly match the previously-visible row.
   */
  public Locator rowForApp(String appPublicId) {
    return rows().filter(new Locator.FilterOptions().setHasText(appPublicId));
  }

  public Locator emptyMessage() {
    return table().getByText(EMPTY_TABLE_MESSAGE);
  }

  public Locator appCellOf(Locator row) {
    return row.getByRole(AriaRole.CELL).first();
  }

  /** "Show Contact" button rendered when the contact name has not been lazy-loaded yet. */
  public Locator showContactButtonOf(Locator row) {
    return row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(SHOW_CONTACT_BUTTON_NAME));
  }

  /** Decorative div with no role; prefer asserting the row's overall text content. */
  public Locator contactNameOf(Locator row) {
    return appCellOf(row).locator(".iq-violation-contact-name");
  }

  /** Mid-load spinner — no role/name; CSS scope is the only hook. */
  public Locator contactLoadingSpinnerOf(Locator row) {
    return appCellOf(row).locator(".nx-loading-spinner");
  }

  /** "Error loading contact" — anchored on the text-bearing span, not the icon. */
  public Locator contactErrorTextOf(Locator row) {
    return appCellOf(row).locator(".iq-violation-contact-name-error-text");
  }

  /**
   * Stage cell by the column's accessible name (e.g. "Source", "Build", "Stage Release",
   * "Release"). Looks up the column index from the live header row so a column reorder
   * doesn't desync the locator.
   */
  public Locator stageCellByName(Locator row, String stageColumnName) {
    int oneBasedIndex = headerIndexOf(stageColumnName);
    return row.getByRole(AriaRole.CELL).nth(oneBasedIndex - 1);
  }

  private int headerIndexOf(String columnName) {
    waitForFullHeaderRow(EXPECTED_HEADER_COUNT);
    List<String> texts = tableHeaders().allInnerTexts();
    for (int i = 0; i < texts.size(); i++) {
      if (texts.get(i).trim().equalsIgnoreCase(columnName)) {
        return i + 1;
      }
    }
    throw new IllegalStateException("Column header '" + columnName + "' not found in report table.");
  }

  public Locator buildCellOf(Locator row) {
    return stageCellByName(row, BUILD_COLUMN_NAME);
  }

  public Locator stageReleaseCellOf(Locator row) {
    return stageCellByName(row, STAGE_RELEASE_COLUMN_NAME);
  }

  public Locator releaseCellOf(Locator row) {
    return stageCellByName(row, RELEASE_COLUMN_NAME);
  }

  public Locator sourceCellOf(Locator row) {
    return stageCellByName(row, SOURCE_COLUMN_NAME);
  }

  /** {@code NxSmallThreatCounter} is style-only with no anchoring ARIA role — CSS exception. */
  public Locator stageCellThreatCounters(Locator stageCell) {
    return stageCell.locator(".nx-small-threat-counter");
  }

  /** Accessible name is "View Report" or "Report" depending on developer-dashboard state. */
  public Locator buildReportLinkOf(Locator row) {
    return buildCellOf(row).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(REPORT_LINK_NAME_PATTERN));
  }

  /** Sibling Priorities link, present only when {@code isDeveloperDashboardEnabled} is true. */
  public Locator buildLifecyclePrioritiesLinkOf(Locator row) {
    return buildCellOf(row).getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName(LIFECYCLE_PRIORITIES_LINK_NAME));
  }

  /** "View Priorities" link rendered in the {@code isDeveloper && isDeveloperDashboardEnabled} branch. */
  public Locator buildDeveloperOnlyPrioritiesLinkOf(Locator row) {
    return buildCellOf(row).getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName(DEVELOPER_PRIORITIES_LINK_NAME));
  }

  public Locator noViolationsTextIn(Locator stageCell) {
    return stageCell.getByText(NO_VIOLATIONS_TEXT);
  }

  public Locator sourcePendingTextIn(Locator sourceCell) {
    return sourceCell.getByText(PENDING_TEXT);
  }

  /** Decorative counter div with no role/name — CSS scoping is the only reliable selector. */
  public Locator criticalCounterIn(Locator stageCell) {
    return stageCell.locator(".nx-small-threat-counter--critical");
  }

  public Locator counterCategoryIn(Locator counter) {
    return counter.locator(".nx-small-threat-counter__category");
  }

  private static final int EXPECTED_HEADER_COUNT = 6;

  /** CSS-anchored — {@code getByRole(COLUMNHEADER)} would also match the inner sort buttons. */
  public Locator tableHeaders() {
    return locator("#" + TABLE_ID + " thead > tr > th");
  }

  // Anchored on the th's stable {@code iq-report-*-cell} class — the inner sort button's
  // accessible name flips across clicks ("Application unsorted" → "ascending"…), so role-name
  // matchers go stale.

  public Locator applicationHeaderCell() {
    return locator("#" + TABLE_ID + " thead th.iq-report-app-cell");
  }

  public Locator organizationHeaderCell() {
    return locator("#" + TABLE_ID + " thead th.iq-report-org-cell");
  }

  /**
   * Stage headers have no stable class; resolve via the live header lookup so a column reorder
   * doesn't desync the locator (mirrors {@link #stageCellByName}).
   */
  public Locator sourceColumnHeader() {
    return tableHeaders().nth(headerIndexOf("Source") - 1);
  }

  public Locator sortButtonOf(Locator headerCell) {
    return headerCell.getByRole(AriaRole.BUTTON);
  }

  public void clickApplicationSort() {
    sortButtonOf(applicationHeaderCell()).click();
  }

  public void clickOrganizationSort() {
    sortButtonOf(organizationHeaderCell()).click();
  }

  /** Filter is debounced; callers wait on the row count, not on a sleep. */
  public void typeFilter(String text) {
    filterInput().fill(text);
  }

  public void clearFilter() {
    filterInput().fill("");
  }

  /** Stage columns mount async after {@code loadStagesAndReports} — wait before sampling. */
  public void waitForFullHeaderRow(int expectedHeaderCount) {
    assertThat(tableHeaders()).hasCount(expectedHeaderCount,
        new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  /**
   * Atomic snapshot via {@code allInnerTexts()} — {@code count()} + {@code nth(i).innerText()} would race the async
   * mount.
   */
  public List<String> headerTexts(int expectedHeaderCount) {
    waitForFullHeaderRow(expectedHeaderCount);
    return tableHeaders().allInnerTexts().stream().map(String::trim).toList();
  }
}
