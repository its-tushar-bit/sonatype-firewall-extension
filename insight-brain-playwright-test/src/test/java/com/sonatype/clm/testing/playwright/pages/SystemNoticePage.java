/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
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

  public Locator explanation() {
    return locator("#system-notice-explanation");
  }

  public Locator noticeText() {
    return byLabel("Notice Text");
  }

  public Locator displayToggle() {
    return byRole(AriaRole.CHECKBOX, "Enable Notice Display");
  }

  public Locator saveButton() {
    return byRole(AriaRole.BUTTON, "Save");
  }

  public Locator cancelButton() {
    return byRole(AriaRole.BUTTON, "Cancel");
  }

  public Locator messageInput() {
    return byLabel("Notice Text");
  }

  public Locator enabledToggle() {
    return byRole(AriaRole.CHECKBOX, "Enable Notice Display");
  }
}
