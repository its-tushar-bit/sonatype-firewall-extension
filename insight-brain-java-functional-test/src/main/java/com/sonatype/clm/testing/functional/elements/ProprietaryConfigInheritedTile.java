/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.WebElementCondition;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ProprietaryConfigInheritedTile
    extends BasicElement<ProprietaryConfigInheritedTile>
{
  private static final String CONFIG_HIERARCHY_SELECTOR = ".inherited-proprietary-component-matchers";

  public ProprietaryConfigInheritedTile() {
    super();
  }

  public ProprietaryConfigInheritedList proprietaryConfigInheritedList(int num) {
    return new ProprietaryConfigInheritedList(selector, CONFIG_HIERARCHY_SELECTOR,
        nthChild(num + 2 /* some other elements have the same parent */));
  }

  public ElementsCollection proprietaryConfigInheritedLists() {
    return children(CONFIG_HIERARCHY_SELECTOR);
  }

  public static WebElementCondition inheritedText(String parent) {
    return Condition.text("INHERITED FROM " + parent);
  }
}
