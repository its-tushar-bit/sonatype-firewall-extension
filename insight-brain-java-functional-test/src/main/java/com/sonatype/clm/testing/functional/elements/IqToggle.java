/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exist;

public class IqToggle
{
  private final SelenideElement element;

  public IqToggle(SelenideElement element) {
    this.element = element;
  }

  public SelenideElement label() {
    return element.$(".nx-toggle__content").find("span");
  }

  public String tooltipText() {
    return checkedElement().find("span").find("span").attr("aria-label");
  }

  private SelenideElement checkedElement() {
    return element.$("label");
  }

  public SelenideElement shouldBeOn() {
    checkedElement().shouldNotHave(cssClass("tm-unchecked"));
    checkedElement().shouldHave(cssClass("tm-checked"));
    return element;
  }

  public SelenideElement shouldBeOff() {
    checkedElement().shouldNotHave(cssClass("tm-checked"));
    checkedElement().shouldHave(cssClass("tm-unchecked"));
    return element;
  }

  public void click() {
    this.element.click();
  }

  public SelenideElement shouldExist() {
    return element.should(exist);
  }

  public SelenideElement shouldNotExist() {
    return element.shouldNot(exist);
  }

  public SelenideElement shouldBeDisabled() {
    checkedElement().shouldHave(cssClass("nx-toggle--disabled"));
    return element;
  }

  public SelenideElement shouldNotBeDisabled() {
    checkedElement().shouldNotHave(cssClass("nx-toggle--disabled"));
    return element;
  }
}
