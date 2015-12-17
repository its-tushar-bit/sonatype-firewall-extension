/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class DoubleColumnPicker
{
  public static SelenideElement root() {
    return $("double-column-picker");
  }

  public static SelenideElement filter() {
    return root().$(".filter-row input");
  }

  public static Checkbox checkAllLeft() {
    return new Checkbox(root().$$(".info-row .tools label.checkbox").get(0));
  }

  public static Checkbox checkAllRight() {
    return new Checkbox(root().$$(".info-row .tools label.checkbox").get(1));
  }

  public static SelenideElement pickCheckedItemsButton() {
    return root().$$(".info-row .tools button").get(0);
  }

  public static SelenideElement unpickCheckedItemsButton() {
    return root().$$(".info-row .tools button").get(1);
  }

  public static SelenideElement availableItemList() {
    return root().$(".list-row .available-list");
  }

  public static SelenideElement pickedItemList() {
    return root().$(".list-row .picked-list");
  }

  public static ElementsCollection availableItems() {
    return availableItemList().$$("label");
  }

  public static ElementsCollection pickedItems() {
    return pickedItemList().$$("label");
  }

  public static DoubleColumnPickerListItem availableItem(int num) {
    return new DoubleColumnPickerListItem(availableItems().get(num));
  }

  public static DoubleColumnPickerListItem pickedItem(int num) {
    return new DoubleColumnPickerListItem(pickedItems().get(num));
  }

  public static class DoubleColumnPickerListItem
  {
    private SelenideElement root;

    public DoubleColumnPickerListItem(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement name() {
      return root.$("span");
    }

    public Checkbox checkbox() {
      return new Checkbox(root);
    }
  }
}
