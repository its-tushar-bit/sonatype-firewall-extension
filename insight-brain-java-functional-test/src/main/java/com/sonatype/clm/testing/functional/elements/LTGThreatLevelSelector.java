/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LTGThreatLevelSelector
{
  public static SelenideElement root() {
    return $("ltg-threat-level-selector");
  }

  public static SelenideElement selectedThreatLevel() {
    return root().$(".selected-threat-level");
  }

  public static SelenideElement caretButton() {
    return root().$(".caret-button");
  }

  public static SelenideElement threatLevelList() {
    return root().$("ul.dropdown-menu");
  }

  public static ElementsCollection threatLevelListItems() {
    return root().$$("ul.dropdown-menu li");
  }

  public static SelenideElement threatLevelListItem(int num) {
    return threatLevelListItems().get(num);
  }
}
