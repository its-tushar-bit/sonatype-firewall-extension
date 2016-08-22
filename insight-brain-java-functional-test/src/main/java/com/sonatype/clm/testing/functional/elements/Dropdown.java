/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class Dropdown
    extends BasicElement<Dropdown>
{
  private static final String ITEM_SELECTOR = "ul.dropdown-menu li";

  public Dropdown(String... selectors) {
    super(selectors);
  }

  public SelenideElement selectedItem() {
    return child(".selected-item");
  }

  public ElementsCollection listItems() {
    return children(ITEM_SELECTOR);
  }

  public SelenideElement listItem(int num) {
    return child(ITEM_SELECTOR, nthChild(num + 1));
  }

  public void chooseOption(Option option){
    selectedItem().click();
    listItem(option.row).shouldBe(visible).shouldHave(text(option.value)).click();
  }

  public static class Option {
    private int row;
    private String value;

    public Option(final int row, final String value) {
      this.row = row;
      this.value = value;
    }
  }
}
