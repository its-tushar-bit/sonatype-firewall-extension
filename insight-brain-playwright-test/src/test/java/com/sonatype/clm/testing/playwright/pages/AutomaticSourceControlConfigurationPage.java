/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** Automatic Source Control configuration page ({@code /automaticSourceControlConfiguration}). */
public class AutomaticSourceControlConfigurationPage
    extends BasePage
{
  private static final String ROOT = "#automatic-source-control-configuration-container";

  private static final String TILE = "#automatic-source-control-configuration";

  private static final String TOGGLE_INPUT = "#automatic-source-control-toggle-checkbox";

  public AutomaticSourceControlConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/automaticSourceControlConfiguration";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator tile() {
    return locator(TILE);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(1).setName("Automatic Source Control"));
  }

  public Locator tileHeading() {
    return tile().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(2).setName("Automatic Source Control Configuration"));
  }

  public Locator enabledToggleLabel() {
    return nxToggleLabel(tile(), "Enable Automatic Source Control Configuration");
  }

  public Locator enabledToggleInput() {
    return locator(TOGGLE_INPUT);
  }

  public Locator updateButton() {
    return tile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update").setExact(true));
  }

  public Locator cancelButton() {
    return tile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Cancel").setExact(true));
  }

}
