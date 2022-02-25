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

  public static SelenideElement sidebar() {
    return $(".nx-global-sidebar");
  }

  public static SelenideElement productVersion() {
    return $(".nx-global-sidebar__release");
  }

  public static SelenideElement productLogo() {
    return $(".nx-global-sidebar__logo");
  }

  public static SelenideElement toggleNavigationButton() {
    return $(".nx-global-sidebar__toggle");
  }

  public static void closeNavigationSidebar() {
    SelenideElement sidebarToggle = toggleNavigationButton();
    if (sidebar().has(CLM.CSS_SIDEBAR_CLOSED)) {
      return;
    }

    sidebarToggle.click();
  }

  public static void openNavigationSidebar() {
    SelenideElement sidebarToggle = toggleNavigationButton();
    if (sidebar().has(CLM.CSS_SIDEBAR_OPEN)) {
      return;
    }

    sidebarToggle.click();
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

  public static MainHeaderNavigationButton vulnerabilityDetailsNavigationButton() {
    return new MainHeaderNavigationButton("#vulnerability-navigation-button");
  }

  public static MainHeaderNavigationButton advancedSearchNavigationButton() {
    return new MainHeaderNavigationButton("#search-navigation-button");
  }

  public static MainHeaderNavigationButton firewallNavigationButton() {
    return new MainHeaderNavigationButton("#firewall-navigation-button");
  }

  public static MainHeaderNavigationButton legalNavigationButton() {
    return new MainHeaderNavigationButton("#advanced-legal-navigation-button");
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

  public static SelenideElement mainHeaderButtons() {
    return $("#main-header-buttons");
  }

  public static SelenideElement loginButton() {
    return $("#header-login-button");
  }

  public static SelenideElement backButton() {
    return $("#menu-bar__back-button-container .nx-text-link");
  }
}
