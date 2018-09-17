/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ProprietaryComponentMatcher
{
  private final String selector;

  public ProprietaryComponentMatcher(String rootSelector, MatcherType type, String name) {
    this.selector =
        rootSelector + " ul li[data-matcher-type^=\"" + type.name + "\"][data-matcher-value^=\"" + name + "\"]";
  }

  public SelenideElement name() {
    return $(createSelector(selector, "li", nthChild(0)));
  }

  public SelenideElement deleteButton() {
    return $(createSelector(selector, "button"));
  }

  public enum MatcherType
  {
    PACKAGE("Package", 0), REGEX("Regular Expression", 1);
  
    public final String name;
  
    public final int dropdownIndex;
  
    MatcherType(String name, int dropdownIndex) {
      this.name = name;
      this.dropdownIndex = dropdownIndex;
    }
  }
}
