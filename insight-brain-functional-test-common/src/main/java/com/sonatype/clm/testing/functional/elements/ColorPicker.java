/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.insight.brain.model.Color;

import com.codeborne.selenide.SelenideElement;

public class ColorPicker
    extends BasicElement<ColorPicker>
{
  public ColorPicker(String... selectors) {
    super(selectors);
  }

  public SelenideElement color(final Color color) {
    return child("." + color.toValue());
  }

  public SelenideElement selectedColor() {
    return child(".color-picker-row > div.selected");
  }
}
