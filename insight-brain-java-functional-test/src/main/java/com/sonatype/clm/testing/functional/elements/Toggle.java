/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

public class Toggle
    extends BasicElement<Toggle>
{
  public Toggle(String selector) {
    super(selector);
  }

  public SelenideElement input() {
    return child("input[type='checkbox']");
  }

  public SelenideElement toggleButton() {
    return child(".toggle");
  }

  private SelenideElement conditionTarget(WebElementCondition condition) {
    return Condition.checked.equals(condition) ? input() : toggleButton();
  }

  @Override
  public Toggle shouldBe(WebElementCondition... conditions) {
    for (WebElementCondition condition : conditions) {
      conditionTarget(condition).shouldBe(condition);
    }
    return this;
  }

  @Override
  public Toggle shouldNotBe(WebElementCondition... conditions) {
    for (WebElementCondition condition : conditions) {
      conditionTarget(condition).shouldNotBe(condition);
    }
    return this;
  }

  public boolean isChecked() {
    return input().isSelected();
  }

  @Override
  public Toggle click() {
    toggleButton().click();
    return me();
  }
}
