/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** Developer Dashboard Overview "Build Stage Risk Monitoring Summary" table. */
public class DeveloperRiskTablePage
    extends BasePage
{
  /**
   * Stable wrapper id of the {@code NxTableContainer} that holds the risk table — used to
   * scope {@code byRole(TABLE)} so we don't collide with a future {@code
   *
  <table>
   * } rendered in
   * a filter popover, modal, or other dashboard region.
   */
  private static final String TABLE_CONTAINER_ID = "#iq-developer-app-integrations-and-risk-table";

  private static final String FILTER_POPOVER_CLASS = ".risk-table-filter";

  public DeveloperRiskTablePage() {
    super();
  }

  public Locator container() {
    return locator(TABLE_CONTAINER_ID).getByRole(AriaRole.TABLE);
  }

  public Locator searchInput() {
    return container().getByPlaceholder("Search by name");
  }

  public Locator filterToggleButton() {
    return byRole(AriaRole.BUTTON, "Filter");
  }

  public Locator filterPopover() {
    return locator(FILTER_POPOVER_CLASS);
  }

  public Locator filterApplyButton() {
    // Scope to the popover container so we don't collide with a generic "Apply" button rendered
    // elsewhere on the dashboard (the popover wrapper has no ARIA role/name of its own).
    return filterPopover()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Apply"));
  }

  /**
   * Row containing the given application-name link (filter by hasText is sufficient because
   * application names are seeded with {@link com.sonatype.insight.brain.dataaccess.TemporaryEntity#uuid()}
   * suffixes and are unique per test).
   */
  public Locator rowByAppName(String applicationName) {
    return container()
        .locator("tbody .nx-table-row")
        .filter(new Locator.FilterOptions().setHasText(applicationName));
  }

  public Locator applicationLinkInRow(Locator row) {
    return row.getByRole(AriaRole.LINK).first();
  }

  /**
   * The "Configure" button in the row's CI/CD column (2nd cell). The row has two Configure
   * buttons (CI/CD + SCM Feedback) with identical accessible names, so we scope to the cell
   * by column index before resolving the role.
   */
  public Locator cicdConfigureButtonInRow(Locator row) {
    return row.locator("td")
        .nth(1)
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Configure"));
  }

  public Locator scmConfigureButtonInRow(Locator row) {
    return row.locator("td")
        .nth(2)
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Configure"));
  }

  /**
   * "Last Commit" date cell (column index 3) — rendered by
   * {@code formatTimestampToDate(lastCommitTimestamp)}.
   */
  public Locator lastCommitDateCellInRow(Locator row) {
    return row.locator("td").nth(3);
  }

  /**
   * "Last Evaluation" date cell (column index 4) — rendered by
   * {@code formatTimestampToDate(lastEvaluationTimestamp)}.
   */
  public Locator lastEvaluationDateCellInRow(Locator row) {
    return row.locator("td").nth(4);
  }

  public Locator emptyStateCell() {
    return container().getByText("No data available given the applied filters and permissions.");
  }
}
