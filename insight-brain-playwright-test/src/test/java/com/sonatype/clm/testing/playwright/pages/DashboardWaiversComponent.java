/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Dashboard Waivers tab.
 */
public class DashboardWaiversComponent
    extends BasePage
{
  private static final String ROOT = "#dashboard-waivers";

  private static final String ENTRIES = ROOT + " .iq-dashboard-waivers-entries";

  public DashboardWaiversComponent() {
    super();
  }

  public Locator waivers() {
    return locator(ENTRIES + " .iq-dashboard-waiver");
  }

  public Locator firstWaiver() {
    return locator(ENTRIES + " .iq-dashboard-waiver:first-child");
  }

  public Locator waiver(int index) {
    return locator(ENTRIES + " .iq-dashboard-waiver:nth-child(" + (index + 1) + ")");
  }

  public Locator noDataMessage() {
    return locator(ENTRIES + " .nx-table-row:last-child");
  }

  // --------------- Waiver tile cell accessors ---------------

  public Locator threatIndicator(int index) {
    return waiver(index).locator(".iq-threat-cell .nx-threat-indicator");
  }

  public Locator threatNumber(int index) {
    return waiver(index).locator(".iq-threat-cell .nx-threat-number");
  }

  public Locator createTime(int index) {
    return waiver(index).locator(".nx-cell:nth-child(2)");
  }

  public Locator expiryTime(int index) {
    return waiver(index).locator(".nx-cell:nth-child(3)");
  }

  public Locator policy(int index) {
    return waiver(index).locator(".nx-cell:nth-child(4)");
  }

  public Locator scope(int index) {
    return waiver(index).locator(".nx-cell:nth-child(5)");
  }

  public Locator component(int index) {
    return waiver(index).locator(".nx-cell:nth-child(6)");
  }

  public Locator upgradeAvailable(int index) {
    return waiver(index).locator(".iq-upgrade-cell");
  }

  // --------------- Pagination ---------------

  public Locator paginatorNextButton() {
    return locator(ROOT + " .nx-table-container__footer >> xpath=//button[@aria-label='next page']");
  }

  public Locator paginatorPreviousButton() {
    return locator(ROOT + " .nx-table-container__footer >> xpath=//button[@aria-label='previous page']");
  }
}
