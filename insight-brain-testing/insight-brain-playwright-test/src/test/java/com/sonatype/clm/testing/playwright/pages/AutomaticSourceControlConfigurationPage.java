/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class AutomaticSourceControlConfigurationPage
    extends BasePage
{
  private static final String ROOT = "#automatic-source-control-configuration-container";

  private static final String TOGGLE_LABEL = "Enable Automatic Source Control Configuration";

  public AutomaticSourceControlConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/automaticSourceControlConfiguration";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Automatic Source Control").setExact(true));
  }

  /** NxToggle split-locator: label for click, input for isChecked. */
  public Locator toggleLabel() {
    return nxToggleLabel(TOGGLE_LABEL);
  }

  public Locator toggleInput() {
    return nxToggleInput(TOGGLE_LABEL);
  }

  public Locator updateButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update").setExact(true));
  }

  public Locator cancelButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator automaticApplicationsLink() {
    return container().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Automatic Applications"));
  }

  public Locator scmOnboardingLink() {
    return container().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Easy SCM Onboarding tool"));
  }
}
