/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class DashboardWaiversComponent
    extends BasePage
{
  private static final String ROOT = "#dashboard-waivers";

  private static final Locator.GetByRoleOptions EXISTING_WAIVERS_TAB_OPTS =
      new Locator.GetByRoleOptions().setName("Existing Waivers");

  private static final Locator.GetByRoleOptions REQUESTED_WAIVERS_TAB_OPTS =
      new Locator.GetByRoleOptions().setName("Requested Waivers");

  private static final Locator.GetByRoleOptions NEXT_PAGE_OPTS =
      new Locator.GetByRoleOptions().setName("next page");

  private static final Locator.GetByRoleOptions PREVIOUS_PAGE_OPTS =
      new Locator.GetByRoleOptions().setName("previous page");

  public DashboardWaiversComponent() {
    super();
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator waivers() {
    return container().getByRole(AriaRole.TABLE).locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator firstWaiver() {
    return waivers().first();
  }

  public Locator waiver(int index) {
    return waivers().nth(index);
  }

  /**
   * The first waiver row containing the given application-name substring. Tests use this when
   * the row index is not predictable (e.g. the table may include unrelated waivers from prior
   * setup). Mirrors {@code ApplicationReportPage.violationRowForComponent}.
   */
  public Locator waiverRowForApp(String appNameSubstring) {
    return waivers()
        .filter(new Locator.FilterOptions().setHasText(appNameSubstring))
        .first();
  }

  public Locator noDataMessage() {
    return container().getByRole(AriaRole.TABLE).locator("tbody").getByRole(AriaRole.ROW).last();
  }

  public Locator threatIndicator(int index) {
    return waiver(index).getByRole(AriaRole.CELL).nth(0);
  }

  public Locator threatNumber(int index) {
    return waiver(index).getByRole(AriaRole.CELL).nth(0).locator(".nx-threat-number");
  }

  public Locator createTime(int index) {
    return waiver(index).getByRole(AriaRole.CELL).nth(1);
  }

  public Locator expiryTime(int index) {
    return waiver(index).getByRole(AriaRole.CELL).nth(2);
  }

  public Locator policy(int index) {
    return waiver(index).getByRole(AriaRole.CELL).nth(3);
  }

  public Locator scope(int index) {
    return waiver(index).getByRole(AriaRole.CELL).nth(4);
  }

  public Locator component(int index) {
    return waiver(index).getByRole(AriaRole.CELL).nth(5);
  }

  public Locator upgradeAvailable(int index) {
    return waiver(index).getByRole(AriaRole.CELL).nth(6);
  }

  public Locator existingWaiversTab() {
    return container().getByRole(AriaRole.TAB, EXISTING_WAIVERS_TAB_OPTS);
  }

  public Locator requestedWaiversTab() {
    return container().getByRole(AriaRole.TAB, REQUESTED_WAIVERS_TAB_OPTS);
  }

  public Locator waiverRequestsTable() {
    return container().getByRole(AriaRole.TABLE).last();
  }

  public Locator paginatorNextButton() {
    return container().getByRole(AriaRole.BUTTON, NEXT_PAGE_OPTS);
  }

  public Locator paginatorPreviousButton() {
    return container().getByRole(AriaRole.BUTTON, PREVIOUS_PAGE_OPTS);
  }
}
