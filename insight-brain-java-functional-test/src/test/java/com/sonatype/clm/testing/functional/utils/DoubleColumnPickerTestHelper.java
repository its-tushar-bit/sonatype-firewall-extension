/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;

import org.junit.Assert;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.visible;
import static org.hamcrest.Matchers.isEmptyOrNullString;

public class DoubleColumnPickerTestHelper
{
  public static void assertDoubleColumnPickerDefaultState(int numAvailableItems, int numPickedItems, boolean filterOn) {
    DoubleColumnPicker.root().shouldBe(visible);

    if (filterOn) {
      DoubleColumnPicker.filter().shouldBe(visible);
      Assert.assertThat(DoubleColumnPicker.filter().val(), isEmptyOrNullString());
    }
    else {
      DoubleColumnPicker.filter().shouldNot(exist);
    }

    DoubleColumnPicker.checkAllLeft().shouldBe(visible).shouldNotBe(selected);
    DoubleColumnPicker.checkAllRight().shouldBe(visible).shouldNotBe(selected);

    DoubleColumnPicker.pickCheckedItemsButton().shouldBe(disabled);
    DoubleColumnPicker.unpickCheckedItemsButton().shouldBe(disabled);

    DoubleColumnPicker.availableItemList().shouldBe(visible);
    DoubleColumnPicker.pickedItemList().shouldBe(visible);

    DoubleColumnPicker.pickedItems().shouldHaveSize(numPickedItems);
    DoubleColumnPicker.availableItems().shouldHaveSize(numAvailableItems);
  }

  public static void assertDoubleColumnPickerDefaultState(int numAvailableItems, boolean filterOn) {
    assertDoubleColumnPickerDefaultState(numAvailableItems, 0, filterOn);
  }

    public static void assertDoubleColumnPickerDefaultState(int numAvailableItems) {
    assertDoubleColumnPickerDefaultState(numAvailableItems, true);
  }

}
