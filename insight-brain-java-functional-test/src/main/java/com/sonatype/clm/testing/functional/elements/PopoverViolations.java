/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class PopoverViolations
{
  private SelenideElement element;

  private PopoverViolations(SelenideElement element) {
    this.element = element;
  }

  public static PopoverViolations on(SelenideElement element) {
    return new PopoverViolations($('#' + element.attr("name") + "-popover.in"));
  }

  public void shouldShowMaxLengthError() {
    element.shouldBe(visible).shouldHave(text("Maximum length"));
  }

  public void shouldNotExist() {
    element.shouldNot(exist);
  }

  public void shouldShowInvalidCharactersError() {
    element.shouldBe(visible).shouldHave(text("Use valid characters"));
  }

  public void shouldShowInvalidSpacingError() {
    element.shouldBe(visible).shouldHave(text("No leading, trailing or double spaces or tabs"));
  }

  public void shouldShowRequiredError() {
    element.shouldBe(visible).shouldHave(text("Please enter a value"));
  }

  public void shouldShowError(String message) {
    element.shouldBe(visible).shouldHave(text(message));
  }
}
