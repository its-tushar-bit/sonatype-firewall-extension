/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

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

  @Override
  public Toggle shouldBe(Condition... conditions) {
    for (Condition condition : conditions) {
      if (Condition.checked.equals(condition)) {
        input().shouldBe(condition);
      }
      else {
        toggleButton().shouldBe(condition);
      }
    }
    return this;
  }

  @Override
  public Toggle shouldNotBe(Condition... conditions) {
    for (Condition condition : conditions) {
      if (Condition.checked.equals(condition)) {
        input().shouldNotBe(condition);
      }
      else {
        toggleButton().shouldNotBe(condition);
      }
    }
    return this;
  }

  @Override
  public void click() {
    toggleButton().click();
  }
}
