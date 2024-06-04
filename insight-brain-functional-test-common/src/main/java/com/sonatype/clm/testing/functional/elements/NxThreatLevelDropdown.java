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
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class NxThreatLevelDropdown
{
  private static final String ROOT = ".iq-threat-dropdown-selector";

  public static final int NUM_THREAT_LEVELS = 11;

  public static SelenideElement root() {
    return $(ROOT);
  }

  public static SelenideElement selectedThreatLevel() {
    return $(createSelector(ROOT, ".nx-dropdown__toggle-label"));
  }

  public static SelenideElement caretButton() {
    return $(createSelector(ROOT, ".nx-dropdown__toggle"));
  }

  public static SelenideElement threatLevelList() {
    return $(createSelector(ROOT, ".nx-dropdown-menu"));
  }

  public static ElementsCollection threatLevelListItems() {
    return $$(createSelector(ROOT, ".nx-dropdown-button"));
  }

  public static SelenideElement threatLevelListItem(int num) {
    return $(createSelector(ROOT, ".nx-dropdown-button", nthChild(num + 1)));
  }
}
