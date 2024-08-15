/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebElementCondition;

public class IqSortingHeader
    extends BasicElement<IqSortingHeader>
{
  private static WebElementCondition UP = Condition.cssClass("up");

  private static WebElementCondition DOWN = Condition.cssClass("down");

  public IqSortingHeader(String selector) {
    super(selector);
  }

  public SortArrow sortArrowUp() {
    return new SortArrow(childSelector(".iq-column-sort-icons", ".fa-caret-up"), UP);
  }

  public SortArrow sortArrowDown() {
    return new SortArrow(childSelector(".iq-column-sort-icons", ".fa-caret-down"), DOWN);
  }

  public static class SortArrow
      extends BasicElement<SortArrow>
  {
    private WebElementCondition selectedCondition;

    SortArrow(String selector, WebElementCondition selectedCondition) {
      super(selector);
      this.selectedCondition = selectedCondition;
    }

    public SortArrow shouldBeSelected() {
      return this.shouldHave(selectedCondition);
    }

    public SortArrow shouldNotBeSelected() {
      return this.shouldNotHave(selectedCondition);
    }
  }
}
