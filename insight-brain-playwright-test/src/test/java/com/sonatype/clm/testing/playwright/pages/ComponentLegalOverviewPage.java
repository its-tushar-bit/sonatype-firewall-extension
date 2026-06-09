/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

public class ComponentLegalOverviewPage
    extends BasePage
{
  private static final String ROOT = "#component-legal-overview-details";

  private static final String OBLIGATIONS_TILE = ROOT + " #license-obligations-tile";

  private static final String ATTRIBUTION_TILE = ROOT + " #attribution-summary-tile";

  public ComponentLegalOverviewPage() {
    super();
  }

  public static String url(String publicAppId, String componentHash) {
    return "/assets/index.html#/legal/application/" + publicAppId + "/component/" + componentHash;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator obligationRows() {
    return locator(OBLIGATIONS_TILE + " .nx-accordion__summary-wrapper");
  }

  public Locator obligationStatusAt(int index) {
    return locator(OBLIGATIONS_TILE + " .nx-tile-content--accordion-container details")
        .nth(index)
        .locator("button.nx-segmented-btn__main-btn span");
  }

  public Locator resolveAllButton() {
    return locator("#mark-all-obligations-resolved");
  }

  public Locator attributionAccordions() {
    return locator(ATTRIBUTION_TILE + " .nx-accordion");
  }

  public EditAllObligationsModal clickResolveAllObligations() {
    resolveAllButton().click();
    return new EditAllObligationsModal();
  }

  public Locator licenseObligationsTile() {
    return locator(OBLIGATIONS_TILE);
  }

  public Locator attributionSummaryTile() {
    return locator(ATTRIBUTION_TILE);
  }
}
