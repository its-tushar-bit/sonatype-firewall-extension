/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class UserMenu
{

  public static SelenideElement root() {
    return $("#user-menu ul");
  }

  public static SelenideElement changePassword() {
    return $("#change-password");
  }

  public static SelenideElement logout() {
    return $("#logout");
  }
}
