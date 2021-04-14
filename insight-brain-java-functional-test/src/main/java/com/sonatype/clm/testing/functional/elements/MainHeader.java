/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class MainHeader
{
  public static final Condition CSS_SIDEBAR_OPEN = Condition.cssClass("open");

  public static final Condition CSS_SIDEBAR_CLOSED = Condition.cssClass("closed");

  public static SelenideElement get() {
    return $("main-header");
  }

  public static SelenideElement sidebar() {
    return $(".nx-global-sidebar");
  }

  public static SelenideElement productVersion() {
    return $(".iq-sidebar-nav-footer__product-info");
  }

  public static SelenideElement productLogo() {
    return $(".nx-global-sidebar__logo");
  }

  public static SelenideElement toggleNavigationButton() {
    return $(".nx-global-sidebar__toggle");
  }

  public static void closeNavigationSidebar() {
    SelenideElement sidebarToggle = toggleNavigationButton();
    if (sidebar().has(CSS_SIDEBAR_CLOSED)) {
      return;
    }

    sidebarToggle.click();
  }

  public static void openNavigationSidebar() {
    SelenideElement sidebarToggle = toggleNavigationButton();
    if (sidebar().has(CSS_SIDEBAR_OPEN)) {
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

  public static SelenideElement mainHeaderButtons() {
    return $("#main-header-buttons");
  }

  public static SelenideElement loginButton() {
    return $("#login-header-btn");
  }
}
