/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;

public class TrendDelta extends BasicElement<TrendDelta>
{

  public static final Condition UP = cssClass("up");
  public static final Condition DOWN = cssClass("down");

  public TrendDelta(String selector) {
    super(selector);
  }

  public SelenideElement chevron() {
    return child("i");
  }

  public SelenideElement value() {
    return child(":last-child");
  }

}
