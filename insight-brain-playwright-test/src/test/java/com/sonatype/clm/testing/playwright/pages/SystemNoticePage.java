/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the System Notice Configuration page.
 */
public class SystemNoticePage
    extends BasePage
{
  private static final String ROOT = "#system-notice-configuration";

  public SystemNoticePage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/systemNoticeConfiguration";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName("System Notice").setExact(true));
  }

  public Locator tileHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Configure System Notice").setExact(true));
  }

  public Locator explanation() {
    return locator("#system-notice-explanation");
  }

  public Locator noticeText() {
    return byLabel("Notice Text");
  }

  public Locator enabledToggle() {
    return nxToggleLabel(container(), "Enable Notice Display");
  }

  public Locator enabledToggleInput() {
    return nxToggleInput(container(), "Enable Notice Display");
  }

  public Locator updateButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update").setExact(true));
  }

  public Locator cancelButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }
}
