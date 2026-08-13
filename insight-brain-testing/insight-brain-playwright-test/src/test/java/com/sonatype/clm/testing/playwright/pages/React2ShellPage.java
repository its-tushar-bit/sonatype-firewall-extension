/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class React2ShellPage
    extends BasePage
{
  private static final String ROOT = ".iq-react2shell-page";

  public React2ShellPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/reports/react2shell";
  }

  /** CSS class root — no ARIA role; used to scope all interactions. */
  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(1).setName("React2Shell Impact Report"));
  }

  public Locator impactSummaryHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(2).setName("Impact Summary"));
  }

  /** No ARIA role — used only to scope {@link #summaryTileTitle} queries. */
  public Locator summaryTilesContainer() {
    return container().locator(".iq-react2shell-summary-tiles");
  }

  public Locator summaryTileTitle(String title) {
    return summaryTilesContainer().getByText(title, new Locator.GetByTextOptions().setExact(true));
  }

  public Locator impactTable() {
    return container().getByRole(AriaRole.TABLE);
  }

  /** Scoped to thead to exclude tbody cell values from column-header text checks. */
  public Locator tableHeadSection() {
    return impactTable().locator("thead");
  }

  /**
   * Numeric value displayed inside a summary tile identified by its visible title.
   * Anchored on CSS class: the tile has no ARIA role; value and title are sibling divs.
   */
  public Locator summaryTileValue(String tileTitle) {
    return summaryTilesContainer()
        .locator(".iq-react2shell-summary-tiles__tile")
        .filter(new Locator.FilterOptions().setHasText(tileTitle))
        .locator(".iq-react2shell-summary-tiles__value");
  }

  public Locator tableEmptyMessage() {
    return container().getByText("No impact data available.");
  }
}
