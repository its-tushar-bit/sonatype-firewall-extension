/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Dashboard Waiver Requests tab.
 */
public class DashboardWaiverRequestsComponent
    extends BasePage
{
  private static final String ROOT = "#dashboard-waivers";

  private static final String TABLE = ROOT + " #iq-dashboard-waiver-requests-table";

  public DashboardWaiverRequestsComponent() {
    super();
  }

  /**
   * Expected text contract for one row in the waiver-requests table. Used by
   * {@link #shouldShowRequestRow(int, ExpectedRow)} so test classes can assert a row in one
   * call site rather than repeating seven {@code containsText} lines per row.
   * <p>
   * Any field set to {@code null} is treated as "don't assert this column" — useful when an
   * upstream JSON fixture only fills in some columns for a given scenario.
   */
  public record ExpectedRow(
      String threatNumber,
      String createTime,
      String requester,
      String policy,
      String scope,
      String component,
      String status)
  {
  }

  public Locator waiverRequests() {
    return locator(TABLE + " .iq-dashboard-waiver-request");
  }

  public Locator firstWaiverRequest() {
    return locator(TABLE + " .iq-dashboard-waiver-request:first-child");
  }

  public Locator waiverRequest(int index) {
    return locator(TABLE + " .iq-dashboard-waiver-request:nth-child(" + (index + 1) + ")");
  }

  public Locator noDataMessage() {
    return locator(ROOT + " .iq-dashboard-waivers-entries .nx-table-row:last-child");
  }

  // --------------- Waiver request tile cell accessors ---------------

  public Locator threatIndicator(int index) {
    return waiverRequest(index).locator(".iq-threat-cell .nx-threat-indicator");
  }

  public Locator threatNumber(int index) {
    return waiverRequest(index).locator(".iq-threat-cell .nx-threat-number");
  }

  public Locator createTime(int index) {
    return waiverRequest(index).locator(".nx-cell:nth-child(2)");
  }

  public Locator requester(int index) {
    return waiverRequest(index).locator(".nx-cell:nth-child(3)");
  }

  public Locator policy(int index) {
    return waiverRequest(index).locator(".nx-cell:nth-child(4)");
  }

  public Locator scope(int index) {
    return waiverRequest(index).locator(".nx-cell:nth-child(5)");
  }

  public Locator component(int index) {
    return waiverRequest(index).locator(".nx-cell:nth-child(6)");
  }

  public Locator status(int index) {
    return waiverRequest(index).locator(".nx-cell:nth-child(7)");
  }

  // --------------- Composite assertions ---------------

  /**
   * Wait for the dashboard spinner to disappear before reading from the table. The dashboard
   * paints a spinner inside {@code #dashboard-container} while the requests query is in flight;
   * reading the table before it's gone risks asserting on stale or empty rows.
   * <p>
   * Scoped to {@link #ROOT} (the waivers tab), so it never collides with the filter-drawer
   * spinner — which would trip Playwright strict mode.
   */
  public void waitUntilLoaded() {
    assertThat(locator(ROOT + " .nx-loading-spinner").first())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

}
