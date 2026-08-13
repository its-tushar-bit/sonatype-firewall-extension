/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class EditRoiConfigurationPage
    extends BasePage
{
  // role=main is claimed by every route; anchor by id.
  private static final String ROOT = "#edit-roi-configuration-page";

  public EditRoiConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/roiConfiguration/edit";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Return on Investment Configuration").setExact(true));
  }

  public Locator baselineDaysInput() {
    return container().getByLabel("Average days to resolve a violation",
        new Locator.GetByLabelOptions().setExact(true));
  }

  public Locator dailyRiskInput() {
    return container().getByLabel("Estimated cost to the organization per day of unresolved violations",
        new Locator.GetByLabelOptions().setExact(true));
  }

  public Locator updateButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update").setExact(true));
  }

  public Locator cancelLink() {
    return container().getByRole(AriaRole.LINK, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator restoreDefaultsButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Restore Default Values").setExact(true));
  }

  /** NxModal portals outside {@link #container()} — anchor on {@code page} and filter by heading. */
  public Locator restoreDefaultsModal() {
    return page.getByRole(AriaRole.DIALOG)
        .filter(new Locator.FilterOptions().setHas(page.getByRole(AriaRole.HEADING,
            new Page.GetByRoleOptions().setName("Restore Default Values").setExact(true))));
  }

  public Locator restoreDefaultsModalRestoreButton() {
    return restoreDefaultsModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Restore").setExact(true));
  }

  public Locator restoreDefaultsModalCancelButton() {
    return restoreDefaultsModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator backButton() {
    return byRole(AriaRole.LINK, "Back");
  }

  public Locator infoAlertText() {
    return container()
        .getByText("ROI values are displayed in the Lifecycle and Repository Firewall dashboards.");
  }

  public Locator lifecycleMetricsHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Lifecycle Metrics").setExact(true).setLevel(2));
  }

  public Locator malwareAttacksPreventedInput() {
    return container().getByLabel("Detected violations for security-malicious components.");
  }

  public Locator namespaceAttacksPreventedInput() {
    return container().getByLabel("Detected violations for namespace-conflict components.");
  }

  public Locator safeComponentsAutoSelectedInput() {
    return container().getByLabel("Policy compliant components found when installing dependencies.");
  }

  public Locator validationErrorAlert() {
    return byTestId("edit-roi-configuration-page__alert__validation-error");
  }

  public void openRestoreDefaultsModal() {
    restoreDefaultsButton().click();
  }
}
