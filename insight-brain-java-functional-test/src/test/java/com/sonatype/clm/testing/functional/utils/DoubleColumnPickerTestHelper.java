/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class DoubleColumnPickerTestHelper
{
  public static void assertDoubleColumnPickerDefaultState(
      DoubleColumnPicker picker,
      int numAvailableItems,
      int numPickedItems,
      boolean filterOn)
  {
    picker.shouldBe(visible);

    if (filterOn) {
      picker.filter().shouldBe(visible);
      assertThat(picker.filter().val()).isNullOrEmpty();
    }
    else {
      picker.filter().shouldNot(exist);
    }

    picker.checkAllLeft().shouldBe(visible).shouldNotBe(selected);
    picker.checkAllRight().shouldBe(visible).shouldNotBe(selected);

    picker.pickCheckedItemsButton().shouldBe(disabled);
    picker.unpickCheckedItemsButton().shouldBe(disabled);

    picker.availableItemList().shouldBe(visible);
    picker.pickedItemList().shouldBe(visible);

    picker.pickedItems().shouldHave(size(numPickedItems));
    picker.availableItems().shouldHave(size(numAvailableItems));
  }

  public static void assertDoubleColumnPickerDefaultState(
      DoubleColumnPicker picker,
      int numAvailableItems,
      boolean filterOn)
  {
    assertDoubleColumnPickerDefaultState(picker, numAvailableItems, 0, filterOn);
  }

  public static void assertDoubleColumnPickerDefaultState(DoubleColumnPicker picker, int numAvailableItems) {
    assertDoubleColumnPickerDefaultState(picker, numAvailableItems, true);
  }
}
