/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.cssClass;

public class TileSimpleList
{
  public static final WebElementCondition CLICKABLE = cssClass("iq-list--clickable");

  public SelenideElement root;

  public TileSimpleList(SelenideElement root) {
    this.root = root;
  }

  public ElementsCollection elements() {
    return root.$$("li:not(.iq-list__item--empty)");
  }

  public SelenideElement noElementsMessage() {
    return root.$("li.iq-list__item--empty");
  }

  public TileSimpleListElement element(int num) {
    return new TileSimpleListElement(elements().get(num));
  }

  public SelenideElement subsectionHeader() {
    return root.$(".iq-list__title");
  }

  public SelenideElement emptyDescriptor() {
    return root.$(".iq-list__item--empty");
  }

  public static class TileSimpleListElement
  {
    public SelenideElement root;

    public TileSimpleListElement(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement icon() {
      return root.$(".test-list-item-title .fa, .test-list-item-title .hexagon");
    }

    public SelenideElement name() {
      return root.$(".test-list-item-title");
    }

    public SelenideElement description() {
      return root.$(".iq-list__subtext");
    }

    public SelenideElement chevron() {
      return root.$(".fa-chevron-right");
    }
  }
}
