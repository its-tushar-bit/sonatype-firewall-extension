/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

/**
 * CLM Radio Widget. Uses a pseudo element for the radio therefore clicks cannot be processed by the
 * Chrome Webdriver on {@link #input()}
 */
public class Radio
{
  private SelenideElement labelElement;

  public Radio(SelenideElement labelElement) {
    this.labelElement = labelElement;
  }

  private SelenideElement input() {
    return labelElement.$("input[type='radio']");
  }

  private SelenideElement label() {
    return labelElement.$("input[type='radio'] + span");
  }

  public void click() {
    label().click();
  }

  /**
   * {@link #label()} is the only visible element therefore most conditions should be processed on it.
   * Condition.selected should still be tested against the input element
   */
  public Radio shouldBe(Condition... conditions) {
    for (Condition condition : conditions) {
      if (Condition.selected.equals(condition)) {
        input().shouldBe(condition);
      }
      else {
        label().shouldBe(condition);
      }
    }
    return this;
  }

  /**
   * {@link #label()} is the only visible element therefore most conditions should be processed on it.
   * Condition.selected should still be tested against the input element
   */
  public Radio shouldNotBe(Condition... conditions) {
    for (Condition condition : conditions) {
      if (Condition.selected.equals(condition)) {
        input().shouldNotBe(condition);
      }
      else {
        label().shouldNotBe(condition);
      }
    }
    return this;
  }

  /**
   * {@link #label()} is the only visible element therefore most conditions should be processed on it.
   * Condition.selected should still be tested against the input element
   */
  public Radio shouldHave(Condition... conditions) {
    for (Condition condition : conditions) {
      if (Condition.selected.equals(condition)) {
        input().shouldHave(condition);
      }
      else {
        label().shouldHave(condition);
      }
    }
    return this;
  }
}
