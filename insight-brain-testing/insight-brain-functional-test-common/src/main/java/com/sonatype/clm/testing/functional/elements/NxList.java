/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.cssClass;

public class NxList
    extends BasicElement<NxList>
{
  public static final WebElementCondition CLICKABLE = cssClass("nx-list__item--clickable");

  private SelenideElement element;

  public NxList(SelenideElement element) {
    this.element = element;
  }

  public ElementsCollection elements() {
    return element.$$("li:not(.nx-list__item--empty)");
  }

  public SelenideElement noElementsMessage() {
    return element.$("li.nx-list__item--empty");
  }

  public NxList.NxListItem element(int num) {
    return new NxList.NxListItem(elements().get(num));
  }

  public SelenideElement emptyDescriptor() {
    return element.$(".nx-list__item--empty");
  }

  public static class NxListItem
  {
    public SelenideElement root;

    public NxListItem(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement icon() {
      return root.$(".nx-list__item .nx-icon");
    }

    public SelenideElement name() {
      return root.$(".nx-list__text");
    }

    public SelenideElement description() {
      return root.$(".nx-list__subtext");
    }

    public SelenideElement chevron() {
      return root.$(".nx-chevron.nx-icon");
    }
  }
}
