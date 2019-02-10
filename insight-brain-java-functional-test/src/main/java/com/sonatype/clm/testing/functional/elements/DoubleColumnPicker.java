/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class DoubleColumnPicker
    extends BasicElement<DoubleColumnPicker>
{
  private static final String AVAILABLE_ITEM_LIST = ".list-row .available-list";

  private static final String AVAILABLE_ITEM = createSelector(AVAILABLE_ITEM_LIST, "iq-checkbox");

  private static final String PICKED_ITEM_LIST = ".list-row .picked-list";

  private static final String PICKED_ITEM = createSelector(PICKED_ITEM_LIST, "iq-checkbox");

  private static final String ROOT = "double-column-picker";

  public DoubleColumnPicker() {
    super(ROOT);
  }

  public SelenideElement filter() {
    return child(".filter-row input");
  }

  public IqCheckbox checkAllLeft() {
    return new IqCheckbox(children(".info-row .tools iq-checkbox").get(0));
  }

  public IqCheckbox checkAllRight() {
    return new IqCheckbox(children(".info-row .tools iq-checkbox").get(1));
  }

  public SelenideElement pickCheckedItemsButton() {
    return children(".info-row .tools button").get(0);
  }

  public SelenideElement unpickCheckedItemsButton() {
    return children(".info-row .tools button").get(1);
  }

  public SelenideElement availableItemList() {
    return child(AVAILABLE_ITEM_LIST);
  }

  public ElementsCollection availableItems() {
    return children(AVAILABLE_ITEM);
  }

  public SelenideElement pickedItemList() {
    return child(PICKED_ITEM_LIST);
  }

  public ElementsCollection pickedItems() {
    return children(PICKED_ITEM);
  }

  public Item availableItem(int num) {
    return new Item(AVAILABLE_ITEM, nthChild(num + 1));
  }

  public Item pickedItem(int num) {
    return new Item(PICKED_ITEM, nthChild(num + 1));
  }

  public static class Item
      extends IqCheckbox
  {
    public Item(String... selectors) {
      super($(createSelector(selectors)));
    }

    @Override
    public Item hover() {
      element.$("ng-transclude span").hover();
      return this;
    }
  }
}
