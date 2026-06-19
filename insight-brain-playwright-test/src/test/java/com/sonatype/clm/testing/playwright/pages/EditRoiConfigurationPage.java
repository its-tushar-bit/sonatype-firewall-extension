/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class EditRoiConfigurationPage
    extends BasePage
{
  private static final String ROOT = "#edit-roi-configuration-page";

  private static final String RESTORE_DEFAULTS_MODAL = "#edit-roi-configuration-page__restore-defaults-modal";

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

  public Locator restoreDefaultsModal() {
    return locator(RESTORE_DEFAULTS_MODAL);
  }

  public Locator restoreDefaultsModalRestoreButton() {
    return locator(RESTORE_DEFAULTS_MODAL + " .restore-defaults-modal__restore-button");
  }

  public Locator restoreDefaultsModalCancelButton() {
    return locator(RESTORE_DEFAULTS_MODAL + " .restore-defaults-modal__cancel-button");
  }

  /** Click the Restore Default Values button and wait for the confirmation modal to render. */
  public void openRestoreDefaultsModal() {
    restoreDefaultsButton().click();
    assertThat(restoreDefaultsModal()).isVisible();
  }

  /**
   * Waits until the in-flight save has settled. The form is wrapped in {@code NxLoadWrapper} which
   * unmounts its children while the save POST is in flight ({@code loading=true}) and remounts them
   * once the save settles, so the Update button becoming visible again is a deterministic UI-level
   * signal that the save has finished.
   */
  public void waitUntilSaved() {
    assertThat(updateButton()).isVisible();
  }
}
