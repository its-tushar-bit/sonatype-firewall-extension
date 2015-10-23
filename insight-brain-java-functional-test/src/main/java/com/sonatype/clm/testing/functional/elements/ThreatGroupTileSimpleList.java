/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class ThreatGroupTileSimpleList
{

  protected SelenideElement root;

  public ThreatGroupTileSimpleList(SelenideElement root) {
    this.root = root;
  }

  public ElementsCollection elements() {
    return root.$$("li");
  }

  public ThreatGroupTileSimpleListElement element(int num) {
    return new ThreatGroupTileSimpleListElement(elements().get(num));
  }

  public SelenideElement ownerName() {
    return root.$(".subsection-header");
  }

  public SelenideElement emptyDescriptor() {
    return root.$(".empty-list");
  }

  public static Condition threatLevel(int threatLevel) {
    return Condition.cssClass("threat-level-" + threatLevel);
  }
  
  public static class ThreatGroupTileSimpleListElement
  {

    public SelenideElement root;

    public ThreatGroupTileSimpleListElement(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement chevron() {
      return root.$(".fa-chevron-right");
    }

    public SelenideElement threatLevel() {
      return root.$(".threat-legend");
    }

    public SelenideElement name() {
      return root.$(".threat-group-title");
    }

  }

}
