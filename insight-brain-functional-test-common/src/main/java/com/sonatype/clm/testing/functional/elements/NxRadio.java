/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.selected;

/**
 * NxRadio Widget. Uses a pseudo element for the radio therefore clicks cannot be processed by the
 * Chrome Webdriver on {@link #input()}
 */
public class NxRadio
{
  private SelenideElement element;

  public NxRadio(SelenideElement element) {
    this.element = element;
  }

  public SelenideElement input() {
    return element.$("input[type='radio']");
  }

  public SelenideElement label() {
    return element;
  }

  public void click() {
    label().click();
  }

  private SelenideElement elementFor(WebElementCondition condition) {
    if (selected.equals(condition) ||
        Condition.enabled.equals(condition) ||
        Condition.disabled.equals(condition))
    {
      return input();
    }
    else {
      return label();
    }
  }

  /**
   * {@link #label()} is the only visible element therefore most conditions should be processed on it.
   * Condition.selected should still be tested against the input element
   */
  public NxRadio shouldBe(WebElementCondition... conditions) {
    for (WebElementCondition condition : conditions) {
      elementFor(condition).shouldBe(condition);
    }
    return this;
  }

  /**
   * {@link #label()} is the only visible element therefore most conditions should be processed on it.
   * Condition.selected should still be tested against the input element
   */
  public NxRadio shouldNotBe(WebElementCondition... conditions) {
    for (WebElementCondition condition : conditions) {
      elementFor(condition).shouldNotBe(condition);
    }
    return this;
  }

  /**
   * {@link #label()} is the only visible element therefore most conditions should be processed on it.
   * Condition.selected should still be tested against the input element
   */
  public NxRadio shouldHave(WebElementCondition... conditions) {
    for (WebElementCondition condition : conditions) {
      elementFor(condition).shouldHave(condition);
    }
    return this;
  }
}
