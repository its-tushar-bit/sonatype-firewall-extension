/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class WaivedComponentUpgradesPage
    extends BasePage
{
  private static final String ROOT = "main#waived-component-upgrades-configuration";

  public WaivedComponentUpgradesPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/waivedComponentUpgradesConfiguration";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Waived Component Upgrades").setExact(true));
  }

  public Locator tileHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Component Upgrade Availability").setExact(true));
  }

  public Locator monitoringToggle() {
    // The toggle text is dynamic ("Enabled"/"Disabled") so we can't filter by literal text via
    // BasePage.nxToggleLabel(). NxToggle puts the `id` prop on the wrapping <label>, so anchoring
    // on the label's stable id scopes this to one specific toggle even if more are added.
    return container().locator("label.nx-toggle#waived-component-upgrade-toggle");
  }

  public Locator monitoringToggleInput() {
    return locator("#waived-component-upgrade-toggle");
  }

  public Locator updateButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update").setExact(true));
  }

  public Locator cancelButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }
}
