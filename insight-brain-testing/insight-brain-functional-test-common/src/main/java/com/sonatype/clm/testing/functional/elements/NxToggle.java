/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.checked;

public class NxToggle
    extends BasicElement<NxToggle>
{
  public NxToggle(String selector) {
    super(selector);
  }

  public SelenideElement label() {
    return child(".nx-toggle__content");
  }

  public SelenideElement input() {
    return child(".nx-toggle__input");
  }

  public SelenideElement shouldBeOn() {
    input().shouldBe(checked);
    return this.getElement();
  }

  public SelenideElement shouldBeOff() {
    input().shouldNotBe(checked);
    return this.getElement();
  }
}
