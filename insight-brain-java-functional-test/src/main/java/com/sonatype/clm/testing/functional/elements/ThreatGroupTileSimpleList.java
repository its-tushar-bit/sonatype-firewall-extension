/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ThreatGroupTileSimpleList
    extends BasicElement<ThreatGroupTileSimpleList>
{
  public ThreatGroupTileSimpleList(String... selectors) {
    super(selectors);
  }

  public ElementsCollection elements() {
    return children("li");
  }

  public ThreatGroupTileSimpleListElement element(int num) {
    return new ThreatGroupTileSimpleListElement(selector, "li", nthChild(num + 1));
  }

  public SelenideElement ownerName() {
    return child(".subsection-header");
  }

  public SelenideElement emptyDescriptor() {
    return child(".empty-list");
  }

  public static Condition threatLevel(int threatLevel) {
    return Condition.cssClass("threat-level-" + threatLevel);
  }

  public static class ThreatGroupTileSimpleListElement
  {
    private final String selector;

    public ThreatGroupTileSimpleListElement(String... selectors) {
      selector = SelectorUtils.createSelector(selectors);
    }

    public SelenideElement chevron() {
      return $(SelectorUtils.createSelector(selector, ".fa-chevron-right"));
    }

    public SelenideElement threatLevel() {
      return $(SelectorUtils.createSelector(selector, ".threat-legend"));
    }

    public SelenideElement name() {
      return $(SelectorUtils.createSelector(selector, ".threat-group-title"));
    }
  }
}
