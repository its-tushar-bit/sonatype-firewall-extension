/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class DropdownSelector
{
  private SelenideElement root;

  public DropdownSelector(SelenideElement root) {
    this.root = root;
  }

  public SelenideElement root() {
    return root;
  }

  public SelenideElement selectedItem() {
    return root().$(".selected-item");
  }

  public ElementsCollection listItems() {
    return root().$$("ul.dropdown-menu li");
  }

  public SelenideElement listItem(int num) {
    return listItems().get(num);
  }
}
