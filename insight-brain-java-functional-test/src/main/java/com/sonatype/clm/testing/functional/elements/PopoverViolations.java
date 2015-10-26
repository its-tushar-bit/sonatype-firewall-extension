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

  public SelenideElement root;

  private PopoverViolations(SelenideElement root) {
    this.root = root;
  }

  public static PopoverViolations on(SelenideElement element) {
    return new PopoverViolations($('#' + element.attr("name") + "-popover.in"));
  }

  public void shouldShowMaxLengthError() {
    root.shouldBe(visible).shouldHave(text("Maximum length"));
  }

  public void shouldNotExist() {
    root.shouldNot(exist);
  }

  public void shouldShowInvalidCharactersError() {
    root.shouldBe(visible).shouldHave(text("Use valid characters"));
  }
}
