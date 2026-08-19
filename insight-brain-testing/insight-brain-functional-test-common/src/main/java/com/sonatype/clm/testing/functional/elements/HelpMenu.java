/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class HelpMenu
    extends BasicElement<HelpMenu>
{
  public HelpMenu() {
    super("#help-menu");
  }

  public SelenideElement dropdownToggle() {
    return $("#help-menu-dropdown button");
  }

  public SelenideElement documentationLink() {
    return $("#documentation-link");
  }

  public SelenideElement supportLink() {
    return $("#support-link");
  }

  public SelenideElement gettingStartedLink() {
    return $("#getting-started-link");
  }
}
