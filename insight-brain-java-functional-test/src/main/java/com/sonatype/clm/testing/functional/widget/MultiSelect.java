/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.widget;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class MultiSelect
    extends BasicElement<MultiSelect>
{
  public MultiSelect(String selector) {
    super(createSelector(selector, "div"));
  }

  public SelenideElement button() {
    return child("button.btn.dropdown-toggle");
  }

  public SelenideElement filter() {
    return child("ul", "input[type=text]");
  }

  public ElementsCollection entries() {
    return children("li[ng-repeat]");
  }

  public SelenideElement entry(int row) {
    // Note we can't use a strict CSS selector here as we do not know whether the filter is visible (thus nth-child
    // cannot be generically used)
    return entries().get(row).find("input");
  }

  public static final WebElementCondition checked = Condition.cssClass(".selected");

  public static final WebElementCondition open = Condition.cssClass(".btn-group.open");
}
