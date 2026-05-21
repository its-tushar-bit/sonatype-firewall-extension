/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Report List (Reports / violations) page rendered by
 * {@code report/react/ReportsPage.jsx}.
 * <p>
 * The page tree is anchored at {@link #ROOT} ({@code NxPageMain id="iq-report-container"}) which
 * contains the {@link #VIOLATION_TABLE} ({@code NxTable id="iq-violation-table"}). All locators
 * are scoped under one of those so a stale chrome element on the page (drawer, modal, banner)
 * never gives a strict-mode violation.
 * <p>
 * Column ordering (see {@code ReportsPage.jsx#allStages}):
 * Application | Organization | Source | Build | Stage Release | Release.
 */
public class ReportListPage
    extends BasePage
{
  private static final String ROOT = "#iq-report-container";

  private static final String VIOLATION_TABLE = ROOT + " #iq-violation-table";

  /** 1-based column index of the BUILD-stage cell in the violations table. */
  public static final int BUILD_COLUMN_INDEX = 4;

  public ReportListPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/reports/violations";
  }

  public Locator container() {
    return locator(ROOT);
  }

  /** Filter input above the table. */
  public Locator filterInput() {
    return byPlaceholder("Filter");
  }

  /** "Load More Results" button rendered when {@code hasMoreResults} is true. */
  public Locator loadButton() {
    return byRole(AriaRole.BUTTON, "Load More Results");
  }

  // --------------- Table rows ---------------

  /**
   * All violation rows in the table body. Anchored under {@link #VIOLATION_TABLE} so the locator
   * never matches rows in unrelated tables (e.g. modals, side panels).
   */
  public Locator rows() {
    return locator(VIOLATION_TABLE + " #iq-violation-table-body .iq-violation-table-row");
  }

  /** First violation row. Disambiguated at the page-object boundary (see authoring guide §4a). */
  public Locator firstRow() {
    return rows().first();
  }

  /**
   * Build-stage cell of the given row. Returned as a sub-locator so callers can further scope
   * (e.g. into the threat counter) without re-querying from {@code page}.
   */
  public Locator buildCellOf(Locator row) {
    return row.locator("td:nth-child(" + BUILD_COLUMN_INDEX + ")");
  }

  /**
   * Report link inside the build-stage cell. Rendered by
   * {@code ReportsPageViolationCell.ReportLink} as {@code <a id="iq-report-link" href="…">}.
   * The link text is conditional: "View Report" normally, "Report" when developer dashboard
   * is enabled — using the stable id avoids the text-conditional failure.
   * Scoped to the row so the id (which appears once per stage cell) does not trip strict mode.
   */
  public Locator buildReportLinkOf(Locator row) {
    return buildCellOf(row).locator("#iq-report-link");
  }

  /**
   * Critical-threat small counter inside a stage cell. Rendered by {@code NxSmallThreatCounter}
   * as {@code <div class="nx-small-threat-counter nx-small-threat-counter--critical">…</div>}.
   * The category label ("Critical") lives in {@code .nx-small-threat-counter__category}.
   */
  public Locator criticalCounterIn(Locator stageCell) {
    return stageCell.locator(".nx-small-threat-counter--critical");
  }

  /** Category label ("Critical" / "Severe" / "Moderate") inside a small threat counter. */
  public Locator counterCategoryIn(Locator counter) {
    return counter.locator(".nx-small-threat-counter__category");
  }

  // --------------- Headers ---------------

  public Locator tableHeaders() {
    return locator(VIOLATION_TABLE + " thead > tr > th");
  }

  /**
   * Wait until all column headers are rendered. Stage columns ({@code Source}, {@code Build}, …)
   * mount only after {@code loadStagesAndReports()} resolves {@code availStages}; sampling the
   * header row too early yields only {@code Application} and {@code Organization}.
   */
  public void waitForFullHeaderRow(int expectedHeaderCount) {
    assertThat(tableHeaders()).hasCount(expectedHeaderCount,
        new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  /**
   * Trimmed visible text of every column header in display order. Returns what the user sees
   * (CSS uppercase is preserved by {@code innerText()}).
   * <p>
   * Waits for the full header row before snapshotting so async stage loading cannot return a
   * partial column list.
   */
  public List<String> headerTexts(int expectedHeaderCount) {
    waitForFullHeaderRow(expectedHeaderCount);
    Locator headers = tableHeaders();
    int count = headers.count();
    List<String> texts = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      texts.add(headers.nth(i).innerText().trim());
    }
    return texts;
  }

}
