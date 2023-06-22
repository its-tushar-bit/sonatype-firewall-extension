/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class NxColorPicker
    extends BasicElement<NxColorPicker>
{
  public NxColorPicker(String selector) {
    super(selector);
  }

  public SelenideElement color(String color) {
    return child(".nx-selectable-color--" + color);
  }

  public SelenideElement selectedColor() {
    return child(".nx-color-picker > label.selected > .nx-color-picker__input");
  }
}
