/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Success Metrics landing page rendered by
 * {@code labs/successMetrics/SuccessMetricsReportList.jsx}.
 */
public class SuccessMetricsPage
    extends BasePage
{
  private static final String ROOT = "#success-metrics-report-list";

  public static final String DATA_API_DOC_LINK_HREF =
      "http://links.sonatype.com/products/nxiq/doc/success-metrics-data-rest-api/v2";

  public SuccessMetricsPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/labs/successMetrics";
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

}
