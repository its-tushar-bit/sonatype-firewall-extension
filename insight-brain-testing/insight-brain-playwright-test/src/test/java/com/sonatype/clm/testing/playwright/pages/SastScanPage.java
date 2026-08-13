/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the SAST Scan page
 * ({@code #/application/{applicationPublicId}/sastScan/{sastScanId}}).
 */
public class SastScanPage
    extends BasePage
{
  /**
   * {@code SastScanPage.jsx} renders bare {@code <NxPageMain>} (→ {@code <main class="nx-page-main">})
   * with no additional {@code iq-} class. The {@code :has()} pseudo-class scopes the selector to
   * only match a {@code <main>} that contains {@code .iq_sast_scan_findings__container}, which is
   * unique to the SAST Scan page and not rendered by any other page's {@code <main>}.
   */
  private static final String ROOT = "main.nx-page-main:has(.iq_sast_scan_findings__container)";

  private static final String FINDINGS_CONTAINER = ".iq_sast_scan_findings__container";

  public SastScanPage() {
  }

  public static String url(String appPublicId, String sastScanId) {
    return "/assets/index.html#/application/" + appPublicId + "/sastScan/" + sastScanId;
  }

  /** {@code NxH1} page title — contains "{appPublicId} SAST Scan". */
  public Locator pageHeading() {
    return locator(ROOT).getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  /** {@code div.iq_sast_scan_findings__container} wrapping the findings table. */
  public Locator findingsContainer() {
    return locator(FINDINGS_CONTAINER);
  }

  /** {@code NxH2} "SAST Findings" heading inside the findings container. */
  public Locator findingsHeading() {
    return locator(FINDINGS_CONTAINER).getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
  }

  /** {@code NxTableRow} elements rendered for each SAST finding. */
  public Locator findingRows() {
    return locator(FINDINGS_CONTAINER + " tr.nx-table-row");
  }
}
