/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;

public class InlineEditor
{
  private SelenideElement element;

  public InlineEditor(SelenideElement element) {
    this.element = element;
  }

  public String getValue() {
    return element.find(isEdit() ? "input" : "span").text();
  }

  public void setValue(String value) {
    if (!isEdit()) {
      // open editor
      element.find("span").click();
    }
    element.find("input").shouldBe(visible).setValue(value);
  }

  public boolean isEdit() {
    SelenideElement form = element.find("form");
    return form != null && form.isDisplayed();
  }

  public SelenideElement getElement() {
    return element;
  }
}
