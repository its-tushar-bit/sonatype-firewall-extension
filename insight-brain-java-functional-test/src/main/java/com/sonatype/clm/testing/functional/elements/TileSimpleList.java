/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;

public class TileSimpleList
{
  protected SelenideElement root;

  public TileSimpleList(SelenideElement root) {
    this.root = root;
  }

  public ElementsCollection elements() {
    return root.$$("li");
  }

  public TileSimpleListElement element(int num) {
    return new TileSimpleListElement(elements().get(num));
  }

  public SelenideElement subsectionHeader() {
    return root.$(".subsection-header");
  }

  public SelenideElement emptyDescriptor() {
    return root.$(".empty-list");
  }

  public static class TileSimpleListElement
  {
    public static final Condition CLICKABLE = cssClass("clickable");

    public SelenideElement root;

    public TileSimpleListElement(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement icon() {
      return root.$(".title .fa, .title .hexagon");
    }

    public SelenideElement name() {
      return root.$(".title");
    }

    public SelenideElement description() {
      return root.$(".subtitle");
    }

    public SelenideElement chevron() {
      return root.$(".fa-chevron-right");
    }

  }

}
