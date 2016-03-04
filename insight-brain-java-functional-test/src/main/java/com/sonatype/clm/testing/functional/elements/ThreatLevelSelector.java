/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.selector;

public class ThreatLevelSelector
{
  private static final String ROOT = "threat-level-selector";

  public static final int NUM_THREAT_LEVELS = 11;

  public static SelenideElement root() {
    return $(ROOT);
  }

  public static SelenideElement selectedThreatLevel() {
    return $(selector(ROOT, ".selected-threat-level"));
  }

  public static SelenideElement caretButton() {
    return $(selector(ROOT, ".caret-button"));
  }

  public static SelenideElement threatLevelList() {
    return $(selector(ROOT, "ul.dropdown-menu"));
  }

  public static ElementsCollection threatLevelListItems() {
    return $$(selector(ROOT, "ul.dropdown-menu li"));
  }

  public static SelenideElement threatLevelListItem(int num) {
    return $(selector(ROOT, "ul.dropdown-menu li", nthChild(num + 1)));
  }
}
