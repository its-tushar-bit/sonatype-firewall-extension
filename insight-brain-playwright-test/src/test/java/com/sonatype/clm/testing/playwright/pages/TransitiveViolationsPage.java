/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class TransitiveViolationsPage
    extends BasePage
{
  private static final Locator.GetByRoleOptions BACK_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName(Pattern.compile("Back to |All Reports"));

  private static final Locator.GetByRoleOptions REQUEST_WAIVER_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Request Waiver");

  private static final Locator.GetByRoleOptions WAIVE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Waive Transitive Violations");

  private static final Locator.GetByRoleOptions VIEW_WAIVERS_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("View Existing Waivers");

  private static final Locator.GetByRoleOptions DELETE_WAIVER_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Delete Waiver");

  public TransitiveViolationsPage() {
    super();
  }

  public static String url(String appPublicId, String scanId, String hash) {
    return "/assets/index.html#/application/" + appPublicId + "/" + scanId + "/component/" + hash
        + "/transitiveViolations";
  }

  public Locator container() {
    return page.getByRole(AriaRole.MAIN)
        .filter(
            new Locator.FilterOptions().setHasText("Transitive Violations"));
  }

  public Locator pageTitle() {
    return container().getByRole(AriaRole.HEADING).first();
  }

  public Locator reportInfo() {
    return container().locator(".component-details-header__reportinfo");
  }

  public Locator innerSourceTag(String testId) {
    return byTestId(testId);
  }

  public Locator requestWaiverButton() {
    return container().getByRole(AriaRole.BUTTON, REQUEST_WAIVER_BUTTON_OPTS);
  }

  public Locator waiveButton() {
    return container().getByRole(AriaRole.BUTTON, WAIVE_BUTTON_OPTS);
  }

  public Locator viewWaiversButton() {
    return container().getByRole(AriaRole.BUTTON, VIEW_WAIVERS_BUTTON_OPTS);
  }

  public Locator backButton() {
    return container().locator(".nx-back-button").getByRole(AriaRole.LINK);
  }

  public Locator transitiveViolationsTable() {
    return container().getByRole(AriaRole.TABLE);
  }

  public Locator transitiveViolationRows() {
    return transitiveViolationsTable().locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator requestWaiverPopover() {
    return locator("#request-waive-transitive-violations-popover");
  }

  public Locator waivePopover() {
    return locator("#waive-transitive-violations-popover");
  }

  public Locator componentWaiversPopover() {
    return locator("#component-waivers-container");
  }

  public Locator componentWaiversPopoverTitle() {
    return componentWaiversPopover().getByText("Transitive Component Waivers");
  }

  public Locator componentWaiversDeleteButtons() {
    return componentWaiversPopover().getByRole(AriaRole.BUTTON, CommonButtonOptions.DELETE_BUTTON_OPTS);
  }

  public Locator deleteWaiverConfirmButton() {
    return byRole(AriaRole.DIALOG).getByRole(AriaRole.BUTTON, DELETE_WAIVER_BUTTON_OPTS);
  }

  public Locator requestWaiverPopoverCloseButton() {
    return requestWaiverPopover().getByRole(AriaRole.BUTTON, CommonButtonOptions.CLOSE_BUTTON_OPTS);
  }

  public Locator waivePopoverCloseButton() {
    return waivePopover().getByRole(AriaRole.BUTTON, CommonButtonOptions.CLOSE_BUTTON_OPTS);
  }

  public Locator loadingSpinner() {
    return container().getByRole(AriaRole.STATUS);
  }
}
