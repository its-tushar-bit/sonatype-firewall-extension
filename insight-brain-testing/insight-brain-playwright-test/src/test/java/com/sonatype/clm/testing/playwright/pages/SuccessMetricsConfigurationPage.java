/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class SuccessMetricsConfigurationPage
    extends BasePage
{
  private static final String ROOT = "#success-metrics-configuration-container";

  private static final String TILE = "#success-metrics-configuration";

  public SuccessMetricsConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/successMetricsConfiguration";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator tile() {
    return locator(TILE);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Success Metrics").setExact(true));
  }

  public Locator tileHeading() {
    return tile().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Configure Success Metrics").setExact(true));
  }

  public Locator enabledToggle() {
    return nxToggleLabel(tile(), "Enable Success Metrics");
  }

  public Locator enabledToggleInput() {
    return locator("#success-metrics-toggle");
  }

  public Locator updateButton() {
    return tile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update").setExact(true));
  }

  public Locator cancelButton() {
    return tile().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }
}
