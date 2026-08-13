/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.selected;

/**
 * nx-checkbox Widget. Uses a pseudo element for the checkbox therefore clicks cannot be processed by the
 * Chrome Webdriver on {@link #input()}
 */
public class NxCheckbox
{
  private static final WebElementCondition LABEL_DISABLED = cssClass("nx-radio-checkbox--disabled");

  protected SelenideElement element;

  public NxCheckbox(SelenideElement element) {
    this.element = element;
  }

  public SelenideElement input() {
    return element.$("input[type='checkbox']");
  }

  public SelenideElement label() {
    return element;
  }

  public void click() {
    label().click();
  }

  public NxCheckbox hover() {
    label().hover();
    return this;
  }

  /**
   * {@link #label()} is the only visible element therefore most conditions should be processed on it.
   * Condition.selected should still be tested against the input element
   */
  public NxCheckbox shouldBe(WebElementCondition... conditions) {
    for (WebElementCondition condition : conditions) {
      if (selected.equals(condition)) {
        input().shouldBe(condition);
      }
      else {
        if (condition.equals(disabled)) {
          label().shouldBe(LABEL_DISABLED);
        }
        else {
          label().shouldBe(condition);
        }
      }
    }
    return this;
  }

  /**
   * {@link #label()} is the only visible element therefore most conditions should be processed on it.
   * Condition.selected should still be tested against the input element
   */
  public NxCheckbox shouldNotBe(WebElementCondition... conditions) {
    for (WebElementCondition condition : conditions) {
      if (selected.equals(condition)) {
        input().shouldNotBe(condition);
      }
      else {
        if (condition.equals(disabled)) {
          label().shouldNotBe(LABEL_DISABLED);
        }
        else {
          label().shouldNotBe(condition);
        }
      }
    }
    return this;
  }

  // grammatical convenience methods
  public NxCheckbox shouldHave(WebElementCondition... conditions) {
    return shouldBe(conditions);
  }

  public NxCheckbox shouldNotHave(WebElementCondition... conditions) {
    return shouldNotBe(conditions);
  }
}
