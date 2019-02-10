/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ActionList
    extends BasicElement<ActionList>
{
  public ActionList(String... selectors) {
    super(selectors);
  }

  public ElementsCollection elements() {
    return children("li:not(.iq-list__item--empty)");
  }

  public ActionListElement element(int num) {
    return new ActionListElement(selector, num);
  }

  public SelenideElement emptyDescriptor() {
    return child("li.iq-list__item--empty");
  }

  public class ActionListElement
      extends BasicElement<ActionListElement>
  {
    public ActionListElement(String rootSelector, int num) {
      super(rootSelector, "li:not(.iq-list__item--empty)", nthChild(num + 1));
    }

    public SelenideElement text() {
      return child(".webhook-url");
    }

    public SelenideElement subtext() {
      return child(".iq-list__subtext");
    }

    public SelenideElement chevron() {
      return child(".fa-chevron-right");
    }
  }
}
