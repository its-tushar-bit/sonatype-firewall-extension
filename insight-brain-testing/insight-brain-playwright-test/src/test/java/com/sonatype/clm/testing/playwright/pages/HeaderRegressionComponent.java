/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Regression-only page object for Navigation Header interactions.
 * Divergences are documented per-method.
 */
public class HeaderRegressionComponent
    extends HeaderComponent
{
  public HeaderRegressionComponent() {
    super();
  }

  /** Help dropdown toggle (NxStatefulNavigationDropdown titled "Support Options"). */
  public Locator helpMenuButton() {
    return byRole(AriaRole.BUTTON, "Support Options");
  }

  public Locator helpMenuDropdown() {
    return locator("#help-menu-dropdown");
  }

  public Locator gettingStartedLink() {
    return byRole(AriaRole.LINK, "Getting Started");
  }

  /** "Online Help" link (divergence: manual says "Documentation"). */
  public Locator onlineHelpLink() {
    return byRole(AriaRole.LINK, "Online Help");
  }

  /** "Request Support" link (divergence: manual says "support"). */
  public Locator requestSupportLink() {
    return byRole(AriaRole.LINK, "Request Support");
  }

  /** Named link in the System Preferences dropdown (call {@link #systemConfigMenuButton()}.click() first). */
  public Locator systemConfigMenuLink(String label) {
    return locator("#system-configuration-menu")
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(label).setExact(true));
  }

  /**
   * Menu item in the System Preferences dropdown by its id suffix (e.g. "roles" → {@code #system-configuration-roles}).
   */
  public Locator systemConfigMenuItem(String itemName) {
    return locator("#system-configuration-menu").locator("#system-configuration-" + itemName);
  }

}
