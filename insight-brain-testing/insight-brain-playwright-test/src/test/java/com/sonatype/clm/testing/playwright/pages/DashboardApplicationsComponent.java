/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Dashboard Applications tab.
 * Mirrors the Selenide {@code DashboardApplications} element.
 */
public class DashboardApplicationsComponent
    extends BasePage
{
  private static final String ROOT = "#dashboard-applications";

  public DashboardApplicationsComponent() {
    super();
  }

  /**
   * Clicks the Application name column header (which sorts ascending by name on the first click)
   * and asserts that {@code firstSubstring} is the first row and {@code lastSubstring} is the
   * last row. The Name column is the only sortable column where ties don't matter, so this
   * assert-both-ends shape is meaningful.
   */
  public void sortByApplicationNameAssertEnds(String firstSubstring, String lastSubstring) {
    applicationNameHeader().click();
    assertThat(applications().first()).containsText(firstSubstring);
    assertThat(applications().last()).containsText(lastSubstring);
  }

  /**
   * Clicks a risk column header (first click is descending — see
   * {@code DashboardApplicationsTable.jsx}'s {@code sort('-totalApplicationRisk.<col>')}) and
   * asserts that {@code expectedTopAppSubstring} is the first row, then clicks again to flip to
   * ascending and asserts that the same app is now at the last row.
   *
   * <p>
   * This is the deterministic guarantee the table actually offers: the app with the highest
   * value in the column is at the top (desc) or the bottom (asc). Asserting "at both ends in
   * the same direction" only works when no ties exist in the other apps' values for that column,
   * which is fragile when the seed grows.
   */
  public void sortByRiskColumnDescThenAsc(Locator riskHeader, String expectedTopAppSubstring) {
    riskHeader.click();
    assertThat(applications().first()).containsText(expectedTopAppSubstring);
    riskHeader.click();
    assertThat(applications().last()).containsText(expectedTopAppSubstring);
  }

  public void sortByLowRiskDescThenAsc(String expectedTopAppSubstring) {
    sortByRiskColumnDescThenAsc(lowRiskHeader(), expectedTopAppSubstring);
  }

  public void sortByModerateRiskDescThenAsc(String expectedTopAppSubstring) {
    sortByRiskColumnDescThenAsc(moderateRiskHeader(), expectedTopAppSubstring);
  }

  public void sortBySevereRiskDescThenAsc(String expectedTopAppSubstring) {
    sortByRiskColumnDescThenAsc(severeRiskHeader(), expectedTopAppSubstring);
  }

  public void sortByCriticalRiskDescThenAsc(String expectedTopAppSubstring) {
    sortByRiskColumnDescThenAsc(criticalRiskHeader(), expectedTopAppSubstring);
  }

  public void goToNextPage() {
    nextPageButton().click();
  }

  public void goToPreviousPage() {
    previousPageButton().click();
  }

  public void clickTotalRiskColumnHeader() {
    totalRiskHeader().click();
  }

  public void assertPaginationFirstPageState() {
    assertThat(paginatorBar()).isVisible();
    assertThat(nextPageButton()).isVisible();
    assertThat(previousPageButton()).isHidden();
  }

  public void assertPaginationLastPageState() {
    assertThat(nextPageButton()).isHidden();
    assertThat(previousPageButton()).isVisible();
  }

  public void assertPaginationReturnedToFirstPageState() {
    assertThat(nextPageButton()).isVisible();
    assertThat(previousPageButton()).isHidden();
  }

  /**
   * Asserts the Total Risk cell at row {@code appIndex} contains exactly {@code expectedText}
   * (Playwright normalises whitespace, but does not match substrings — so {@code "1"} will not
   * match a cell whose text is {@code "10"}).
   */
  public void assertTotalRiskCellContains(int appIndex, String expectedText) {
    assertThat(totalRisk(appIndex)).hasText(expectedText);
  }

  public void assertApplicationCount(int expectedCount) {
    assertThat(applications()).hasCount(expectedCount);
  }

  /**
   * Asserts that the table renders {@code expectedAppNames.size()} rows in the exact order given.
   * Each name is a substring match against the row text (matches application name + risk cells).
   */
  public void assertDefaultSortOrder(List<String> expectedAppNames) {
    assertApplicationCount(expectedAppNames.size());
    for (int i = 0; i < expectedAppNames.size(); i++) {
      assertThat(applications().nth(i)).containsText(expectedAppNames.get(i));
    }
  }

  /**
   * Toggles each sortable column header and asserts the expected app appears at the ends:
   *
   * <ul>
   * <li>Name column: {@code nameAscFirst} at first row, {@code nameAscLast} at last row.</li>
   * <li>Each risk column (Low/Moderate/Severe/Critical): the highest-value owner is at the top
   * after the descending click, then at the bottom after the ascending click — see
   * {@link #sortByRiskColumnDescThenAsc(Locator, String)} for why this is the only
   * deterministic shape when ties exist among other apps.</li>
   * </ul>
   *
   * @param nameAscFirst app expected at row 0 after sorting Name asc
   * @param nameAscLast app expected at the last row after sorting Name asc
   * @param highestLowApp app with the highest Low Risk in the seeded data
   * @param highestModerateApp app with the highest Moderate Risk in the seeded data
   * @param highestSevereApp app with the highest Severe Risk in the seeded data
   * @param highestCriticalApp app with the highest Critical Risk in the seeded data
   */
  public void assertAllSortColumns(
      String nameAscFirst,
      String nameAscLast,
      String highestLowApp,
      String highestModerateApp,
      String highestSevereApp,
      String highestCriticalApp)
  {
    sortByApplicationNameAssertEnds(nameAscFirst, nameAscLast);
    sortByLowRiskDescThenAsc(highestLowApp);
    sortByModerateRiskDescThenAsc(highestModerateApp);
    sortBySevereRiskDescThenAsc(highestSevereApp);
    sortByCriticalRiskDescThenAsc(highestCriticalApp);
  }

  /**
   * Walks the multi-page paginator: assert first-page state → next page → toggle Total Risk sort →
   * next page (last) → previous (back to first). At each navigation step, the Total Risk cell in
   * row {@code totalRiskRowIndex} is asserted to contain the expected substring.
   */
  public void walkPaginationFlow(
      int totalRiskRowIndex,
      String page1RiskSubstring,
      String page2RiskSubstring,
      String ascPage1RiskSubstring)
  {
    assertPaginationFirstPageState();
    assertTotalRiskCellContains(totalRiskRowIndex, page1RiskSubstring);

    goToNextPage();
    assertTotalRiskCellContains(totalRiskRowIndex, page2RiskSubstring);

    clickTotalRiskColumnHeader();
    assertTotalRiskCellContains(totalRiskRowIndex, ascPage1RiskSubstring);
    goToNextPage();
    assertPaginationLastPageState();

    goToPreviousPage();
    assertPaginationReturnedToFirstPageState();
    assertTotalRiskCellContains(totalRiskRowIndex, ascPage1RiskSubstring);
  }

  /**
   * Opens the dashboard filter drawer, expands the Policy Threat Level section, applies the
   * given range, collapses the section, and closes the drawer. After this returns, the
   * applications table will have re-fetched and re-rendered.
   */
  public void applyPolicyThreatLevelFilter(int min, int max) {
    DashboardPage dashboard = new DashboardPage();
    dashboard.expandFilter();

    DashboardFiltersComponent filters = new DashboardFiltersComponent();
    filters.twisty(filters.policyThreatLevelFilter()).click();
    filters.setPolicyThreatLevelRange(min, max);
    filters.apply();
    filters.twisty(filters.policyThreatLevelFilter()).click();
    filters.closeFilter();
  }

  /**
   * Runs {@code assertions} and, on assertion/runtime failure, logs the current URL plus a
   * compact table snapshot before re-throwing — invaluable when triaging flaky CI runs.
   */
  public void runWithSnapshotOnFailure(Logger log, String stepName, Runnable assertions) {
    try {
      assertions.run();
    }
    catch (AssertionError | RuntimeException e) {
      log.error("{}: failed. url={} snapshot={}", stepName, page.url(), tableSnapshot(5));
      throw e;
    }
  }

  /**
   * Maximum length of a single row's sanitised text in {@link #tableSnapshot(int)} debug
   * output. Caps the overall snapshot at roughly {@code maxRows * MAX_SNAPSHOT_FIELD_CHARS * 2}
   * so a multi-MB dashboard cannot flood CI logs on a single assertion failure.
   */
  private static final int MAX_SNAPSHOT_FIELD_CHARS = 200;

  /**
   * Returns a compact table snapshot for debug logs on assertion failures. Each row's text is
   * truncated to {@link #MAX_SNAPSHOT_FIELD_CHARS} characters so a slow or oversized
   * dashboard cannot push multi-MB output into CI logs.
   */
  public String tableSnapshot(int maxRows) {
    int rowCount = applications().count();
    int rowsToCapture = Math.min(maxRows, rowCount);
    String rows = IntStream.range(0, rowsToCapture)
        .mapToObj(i -> String.format("row[%d]=%s | totalRisk=%s",
            i,
            sanitizeRowText(application(i).innerText()),
            sanitizeRowText(totalRisk(i).innerText())))
        .collect(Collectors.joining(" || "));
    return String.format("rowCount=%d, captured=%d, rows={%s}", rowCount, rowsToCapture, rows);
  }

  private String sanitizeRowText(String rawText) {
    if (rawText == null) {
      return "";
    }
    String collapsed = rawText.replaceAll("\\s+", " ").trim();
    if (collapsed.length() <= MAX_SNAPSHOT_FIELD_CHARS) {
      return collapsed;
    }
    return collapsed.substring(0, MAX_SNAPSHOT_FIELD_CHARS) + "…(truncated)";
  }

  public Locator results() {
    return locator(ROOT + " tbody");
  }

  public Locator applications() {
    return locator(ROOT + " tbody .iq-dashboard-application-row");
  }

  public Locator application(int index) {
    // Restrict to the main application row class — stage child rows also have IDs starting
    // with "app{index}_" (e.g. "app0_stageApp5stage-release") and would otherwise be matched.
    return locator(ROOT + " tbody tr.iq-dashboard-application-row[id^=\"app" + index + "_\"]");
  }

  public Locator firstApplication() {
    return application(0);
  }

  public Locator noDataMessage() {
    return locator(ROOT + " tbody tr:last-child");
  }

  public Locator applicationNameHeader() {
    return locator(ROOT + " .nx-table-row--header .nx-cell--header:nth-child(1)");
  }

  public Locator totalRiskHeader() {
    return locator(ROOT + " .nx-table-row--header .nx-cell--header:nth-child(2)");
  }

  public Locator criticalRiskHeader() {
    return locator(ROOT + " .nx-table-row--header .nx-cell--header:nth-child(3)");
  }

  public Locator severeRiskHeader() {
    return locator(ROOT + " .nx-table-row--header .nx-cell--header:nth-child(4)");
  }

  public Locator moderateRiskHeader() {
    return locator(ROOT + " .nx-table-row--header .nx-cell--header:nth-child(5)");
  }

  public Locator lowRiskHeader() {
    return locator(ROOT + " .nx-table-row--header .nx-cell--header:nth-child(6)");
  }

  public Locator heatmapCells(int appIndex) {
    return locator(ROOT + " tbody tr[id^=\"app" + appIndex + "_\"]:first-child .iq-cell--heatmap");
  }

  public Locator totalRisk(int appIndex) {
    return application(appIndex).locator(".nx-cell:nth-child(2)");
  }

  public Locator stageRows(int appIndex) {
    return locator(ROOT + " tbody tr[id^=\"app" + appIndex + "_\"].iq-dashboard-application-risk-row");
  }

  public Locator stageLink(int appIndex, int stageIndex) {
    return stageRows(appIndex).nth(stageIndex).locator("a[target=_blank]");
  }

  public void waitForNoDataMessage() {
    assertThat(noDataMessage())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void waitForResults(long timeoutMs) {
    assertThat(results())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(timeoutMs));
  }

  public Locator exportResultsLink() {
    return locator("#export-results");
  }

  public Locator paginatorBar() {
    return locator(ROOT + " .nx-table-container__footer .nx-btn-bar--indeterminate-pagination");
  }

  public Locator nextPageButton() {
    return locator(ROOT + " .nx-table-container__footer >> xpath=//button[@aria-label='next page']");
  }

  public Locator previousPageButton() {
    return locator(ROOT + " .nx-table-container__footer >> xpath=//button[@aria-label='previous page']");
  }

}
