/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class ListWaiversTablePage
    extends BasePage
{
  private static final String ROOT = "#list-waivers-table";

  private static final Locator.GetByRoleOptions EXCLUSION_OPTS =
      new Locator.GetByRoleOptions().setName("Remove auto-waiver for this policy violation");

  public ListWaiversTablePage() {
    super();
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator allRows() {
    return container().locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator autoWaiverRow() {
    return container().locator("tbody")
        .getByRole(AriaRole.ROW)
        .filter(
            new Locator.FilterOptions().setHasText("Auto"));
  }

  public Locator activeWaiverRows() {
    return container().locator("tbody tr:not(.list-auto-waiver-row):not(.list-waivers-row--expired)");
  }

  public Locator expiredWaiverRows() {
    return container().locator("tbody tr.list-waivers-row--expired");
  }

  public Locator autoWaiverTag() {
    return autoWaiverRow().getByText("Auto");
  }

  public Locator autoWaiverExclusionButton() {
    return container().getByRole(AriaRole.BUTTON, EXCLUSION_OPTS);
  }

  public Locator waiverRowCreatedDate(int index) {
    return container().locator("tbody").getByRole(AriaRole.ROW).nth(index).locator(".waiver-row-date-created");
  }

  public Locator waiverRowDeleteButton(int index) {
    return container().locator("tbody")
        .getByRole(AriaRole.ROW)
        .nth(index)
        .getByRole(AriaRole.BUTTON, CommonButtonOptions.DELETE_BUTTON_OPTS);
  }

  public Locator firstDeleteButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.DELETE_BUTTON_OPTS).first();
  }

  public Locator emptyMessage() {
    return container().getByRole(AriaRole.CELL)
        .filter(
            new Locator.FilterOptions().setHasText("don't have any waivers"));
  }

  public Locator emptyMessageLink() {
    return container().getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("help documentation"));
  }

  public Locator loadingSpinner() {
    return container().getByRole(AriaRole.STATUS);
  }

  public Locator errorMessage() {
    return container().getByRole(AriaRole.ALERT);
  }

  public Locator retryButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.RETRY_BUTTON_OPTS);
  }

  public Locator deleteWaiverModal() {
    return page.getByRole(AriaRole.DIALOG);
  }

  public Locator deleteWaiverModalHeading() {
    return deleteWaiverModal().getByRole(AriaRole.HEADING);
  }

}
