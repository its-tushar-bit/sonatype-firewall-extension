/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Success Metrics landing page rendered by
 * {@code labs/successMetrics/SuccessMetricsReportList.jsx}.
 */
public class SuccessMetricsPage
    extends BasePage
{
  private static final String ROOT = "#success-metrics-report-list";

  private static final String BASE_URL = "/assets/index.html#/labs/successMetrics";

  public static final String DATA_API_DOC_LINK_HREF =
      "http://links.sonatype.com/products/nxiq/doc/success-metrics-data-rest-api/v2";

  public SuccessMetricsPage() {
    super();
  }

  public static String url() {
    return BASE_URL;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return locator(ROOT + " .nx-page-title .nx-h1");
  }

  public Locator pageDescription() {
    return locator(ROOT + " .nx-page-title__description .nx-h3");
  }

  public Locator reportsTile() {
    return locator(ROOT + " section.nx-tile");
  }

  public Locator reportsTileHeading() {
    return reportsTile().locator(".nx-tile-header__title .nx-h2");
  }

  public Locator reportsTileSubtitle() {
    return reportsTile().locator(".nx-tile-header__subtitle");
  }

  public Locator dataApiDocLink() {
    return reportsTileSubtitle().locator("a[href='" + DATA_API_DOC_LINK_HREF + "']");
  }

  public Locator addReportButton() {
    return locator(ROOT + " #add-success-metrics-report-btn");
  }

  public Locator reportListItems() {
    return reportsTile().locator(".nx-tile-content .nx-list .nx-list__item");
  }

  public Locator emptyReportListItem() {
    return reportsTile().locator(".nx-tile-content .nx-list .nx-list__item--empty");
  }

  /** URL for an individual Success Metrics report. */
  public static String reportUrl(String reportId) {
    return BASE_URL + "/" + reportId;
  }

  /** Individual report container — bare {@code <div>}, no ARIA role. */
  public Locator reportContainer() {
    return locator("#success-metrics-report");
  }

  /** Individual report page heading (H1). */
  public Locator reportPageHeading() {
    return reportContainer().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(1));
  }

  /** Delete Report button inside the report page-title area. */
  public Locator deleteReportButton() {
    return reportContainer().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Delete Report"));
  }

  /** Report row link in the list — accessible name is the report name. */
  public Locator reportListLink(String reportName) {
    return reportsTile().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName(reportName).setExact(true));
  }
}
