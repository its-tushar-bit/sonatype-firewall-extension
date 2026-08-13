/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class DataInsightsPage
    extends BasePage
{
  // role=main is ambiguous across routes; anchor by id.
  private static final String ROOT = "#labs-data-insights-container";

  public DataInsightsPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/dataInsights";
  }

  public Locator container() {
    return locator(ROOT);
  }

  /** Looker-iframe host — rendered outside NxLoadWrapper; no ARIA role. */
  public Locator labsContainer() {
    return locator("#labs-container");
  }

  /**
   * Anchors on the English error copy because the alert (rendered by {@code LoadWrapper}'s
   * {@code NxLoadError}) exposes no stable {@code data-testid}. The IQ UI is not internationalised
   * today; revisit if i18n is added.
   */
  public Locator enterpriseReportingLicenseErrorMessage() {
    return container().getByRole(AriaRole.ALERT)
        .filter(
            new Locator.FilterOptions().setHasText("Enterprise Reporting feature not supported"));
  }
}
