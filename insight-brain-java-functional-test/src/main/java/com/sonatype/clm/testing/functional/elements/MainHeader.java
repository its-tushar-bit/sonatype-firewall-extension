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
  public static SelenideElement get() {
    return $("main-header");
  }

  public static SelenideElement productVersion() {
    return $(".iq-title__version");
  }

  public static MainHeaderNavigationButton dashboardNavigationButton() {
    return new MainHeaderNavigationButton("#dashboard-navigation-button");
  }

  public static MainHeaderNavigationButton reportingNavigationButton() {
    return new MainHeaderNavigationButton("#reporting-navigation-button");
  }

  public static MainHeaderNavigationButton policiesNavigationButton() {
    return new MainHeaderNavigationButton("#policies-navigation-button");
  }

  public static MainHeaderNavigationButton labsNavigationButton() {
    return new MainHeaderNavigationButton("#labs-navigation-button");
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

  public static SelenideElement mainHeaderButtons() {
    return $("#main-header-buttons");
  }
}
