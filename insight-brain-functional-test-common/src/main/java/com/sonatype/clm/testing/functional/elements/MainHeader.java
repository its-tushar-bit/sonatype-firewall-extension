/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class MainHeader
{
  /**
   * @deprecated Use {@link #menuBar()} instead. The main-header element itself is always present,
   *             but may be empty when embeddable mode is active. Check for the actual menu bar content.
   */
  @Deprecated
  public static SelenideElement get() {
    return $("main-header");
  }

  /**
   * Gets the actual menu bar element inside the header.
   * This element will not be present when embeddable mode is active.
   */
  public static SelenideElement menuBar() {
    return $("#menu-bar");
  }

  public static UserMenu userMenu() {
    return new UserMenu();
  }

  public static SystemConfigMenu systemConfigMenu() {
    return new SystemConfigMenu();
  }

  public static HelpMenu helpMenu() {
    return new HelpMenu();
  }

  public static NotificationMenu notificationsMenu() {
    return new NotificationMenu();
  }

  public static SelenideElement loginButton() {
    return $("#header-login-button");
  }

  public static NxBackButton backButton() {
    return new NxBackButton();
  }
}
