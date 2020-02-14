/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Keys;

import static org.openqa.selenium.Keys.BACK_SPACE;

// SelenideElement#clear, SelenideElement#setValue(String) and SelenideElement#val(String) do not behave as expected
// for React components. This wrapper class works around the unexpected behaviour.
// For future reference see: https://github.com/sonatype/insight-brain/pull/4806#discussion_r378425407
public class ReactTextInput
    extends BasicElement<ReactTextInput>
{
  private final SelenideElement element;

  public ReactTextInput(SelenideElement element) {
    this.element = element;
  }

  @Override
  public SelenideElement getElement() {
    return element;
  }

  // SelenideElement#clear does not actually clear the field.
  // As a workaround we send backspace until the field is empty.
  public ReactTextInput clear() {
    while (!element.getValue().equals("")) {
      element.sendKeys(BACK_SPACE);
    }
    return this;
  }

  // SelenideElement#setValue appends the value instead of replacing it (most likely due to same reason clear not
  // behaving as expected). We clear the field explicitly before setting value.
  public ReactTextInput setValue(String value) {
    clear();
    element.setValue(value);
    return this;
  }

  // Same trouble and same solution with setValue.
  public ReactTextInput val(String val) {
    return setValue(val);
  }

  public void sendKeys(Keys keys) {
    element.sendKeys(keys);
  }

  public void sendKeys(String keys) {
    element.sendKeys(keys);
  }
}
