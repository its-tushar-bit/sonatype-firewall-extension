/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Firewall Repository Results Summary page used in regression tests.
 * Kept separate to avoid merge conflicts on the pre-existing {@link FirewallRepositoryResultsPage}.
 * ({@code firewall.repository-report} UI-router state,
 * URL fragment {@code #/firewall/repository/{repositoryId}/result}).
 * Root element is {@code #repository-results-summary-page}.
 */
public class FirewallRepositoryResultsRegressionPage
    extends BasePage
{
  private static final String ROOT = "#repository-results-summary-page";

  public FirewallRepositoryResultsRegressionPage() {
  }

  public static String url(String repositoryId) {
    return "/assets/index.html#/firewall/repository/" + repositoryId + "/result";
  }

  public Locator container() {
    return locator(ROOT);
  }

  /** "Bulk Waive" button rendered by {@code BulkWaiveButton.jsx} ({@code id="fw-bulk-waive"}). */
  public Locator bulkWaiveButton() {
    return locator("#fw-bulk-waive");
  }

  /** {@code ReportStatusBar} stats row ({@code .iq-indicator-row}). */
  public Locator statsBar() {
    return locator(ROOT + " .iq-indicator-row");
  }

  /** Violations section inside the stats bar ({@code .iq-threat-indicators}). */
  public Locator violationsIndicator() {
    return locator(ROOT + " .iq-threat-indicators");
  }

  /** Component-coverage section inside the stats bar ({@code .iq-coverage-indicator}). */
  public Locator coverageIndicator() {
    return locator(ROOT + " .iq-coverage-indicator");
  }

  /**
   * Quarantined-components section inside the stats bar ({@code .iq-quarantine-indicator}).
   * {@code RepositoryResultsSummaryPage} always passes {@code showQuarantinedSection=true} to
   * {@code ReportStatusBar}, so this section renders for every repository.
   */
  public Locator quarantineIndicator() {
    return locator(ROOT + " .iq-quarantine-indicator");
  }
}
