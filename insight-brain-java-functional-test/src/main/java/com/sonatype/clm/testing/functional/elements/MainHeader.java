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
  public static SelenideElement userMenuToggle() {
    return $("#user-menu-dropdown-toggle");
  }

  public static MainHeaderNavigationButton dashboardNavigationButton() {
    return new MainHeaderNavigationButton("#dashboard-navigation-button");
  }

  public static MainHeaderNavigationButton labsNavigationButton() {
    return new MainHeaderNavigationButton("#labs-navigation-button");
  }

  public static SelenideElement userName() {
    return $("#user-name");
  }
}
