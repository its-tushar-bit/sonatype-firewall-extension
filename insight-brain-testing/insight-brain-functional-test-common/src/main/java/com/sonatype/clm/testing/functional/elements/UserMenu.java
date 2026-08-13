/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class UserMenu
    extends BasicElement<UserMenu>
{
  public UserMenu() {
    super("#user-menu");
  }

  public SelenideElement userName() {
    return child("#user-name");
  }

  public SelenideElement dropdownToggle() {
    return child("#user-menu-dropdown button");
  }

  public SelenideElement changePassword() {
    return child("#change-password");
  }

  public SelenideElement manageUserToken() {
    return child("#user-token-management");
  }

  public SelenideElement userDetails() {
    return child("#user-details");
  }

  public SelenideElement displayTheme() {
    return child("#display-theme");
  }

  public SelenideElement logout() {
    return child("#logout");
  }
}
