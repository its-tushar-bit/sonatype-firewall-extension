/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class AccessTileList
{

  protected SelenideElement root;

  public AccessTileList(SelenideElement root) {
    this.root = root;
  }

  public ElementsCollection elements() {
    return root.$$("tr");
  }

  public AccessTileListElement element(int num) {
    return new AccessTileListElement(elements().get(num));
  }

  public SelenideElement ownerName() {
    return root.$(".subsection-header");
  }

  public SelenideElement emptyDescriptor() {
    return root.$(".empty-list");
  }
  
  public static class AccessTileListElement
  {

    public SelenideElement root;

    public AccessTileListElement(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement chevron() {
      return root.$(".fa-chevron-right");
    }

    public SelenideElement role() {
      return root.$(".role");
    }

    public SelenideElement members() {
      return root.$(".members");
    }

    public SelenideElement userIcon() {
      return root.$(".fa-user");
    }

    public SelenideElement groupIcon() {
      return root.$(".fa-users");
    }

  }

}
