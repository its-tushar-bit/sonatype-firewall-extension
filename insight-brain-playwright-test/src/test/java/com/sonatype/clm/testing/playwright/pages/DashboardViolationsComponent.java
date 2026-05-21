/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Dashboard Violations tab.
 */
public class DashboardViolationsComponent
    extends BasePage
{
  private static final String ROOT = "#dashboard-violations";

  public DashboardViolationsComponent() {
    super();
  }

  // --------------- Row locators ---------------

  public Locator violations() {
    return locator(ROOT + " .iq-dashboard-violation");
  }

  public Locator violation(int index) {
    return locator(ROOT + " .iq-dashboard-violation").nth(index);
  }

  public Locator noDataMessage() {
    return locator(ROOT + " .iq-dashboard-violation-entries .nx-table-row:last-child");
  }

  // --------------- Cell accessors ---------------
  // Column order: Threat(1), Policy(2), Application(3), Component(4), Age(5), chevron(6)

  public Locator threatNumber(int index) {
    return violation(index).locator(".nx-threat-number");
  }

  public Locator componentName(int index) {
    return violation(index).locator("td:nth-child(4)");
  }

  public Locator policyName(int index) {
    return violation(index).locator(".iq-policy-cell");
  }

  public Locator applicationName(int index) {
    return violation(index).locator("td:nth-child(3)");
  }

  public Locator reportTime(int index) {
    return violation(index).locator("td:nth-child(5)");
  }

  // --------------- Pagination ---------------

  public Locator paginatorNextButton() {
    return locator(ROOT + " .nx-table-container__footer >> xpath=//button[@aria-label='next page']");
  }

  public Locator paginatorPreviousButton() {
    return locator(ROOT + " .nx-table-container__footer >> xpath=//button[@aria-label='previous page']");
  }

  // --------------- Actions ---------------

  public void clickViolation(int index) {
    violation(index).click();
  }
}
