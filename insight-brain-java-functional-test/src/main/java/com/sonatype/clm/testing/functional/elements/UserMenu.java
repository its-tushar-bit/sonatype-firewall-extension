/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class UserMenu
    extends BasicElement<UserMenu>
{
  public UserMenu() {
    super("#user-menu");
  }

  public SelenideElement userName() {
    return $("#user-name");
  }

  public SelenideElement dropdownToggle() {
    return $("#user-menu-dropdown-toggle");
  }

  public SelenideElement changePassword() {
    return $("#change-password");
  }

  public SelenideElement userDetails() {
    return $("#user-details");
  }

  public SelenideElement logout() {
    return $("#logout");
  }
}
