/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ActionDropDown
{

  public static SelenideElement root() {
    return $("#action-dropdown");
  }

  public static SelenideElement menu() {
    return root().find(".dropdown-menu");
  }

  public static SelenideElement editOwner() {
    return root().find("#app-org-link");
  }

  public static SelenideElement actionButton() {
    return root().find("button");
  }

}
