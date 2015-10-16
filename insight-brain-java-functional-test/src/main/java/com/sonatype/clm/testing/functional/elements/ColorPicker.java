/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.insight.brain.model.Color;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ColorPicker
{
  private SelenideElement root;

  public ColorPicker(SelenideElement root) {
    this.root = root;
  }

  public SelenideElement root() {
    return root;
  }

  public SelenideElement color(final Color color) {
    return $(root, "." + color.toValue());
  }

  public SelenideElement selectedColor() {
    return $(root, ".color-picker-row > div.selected");
  }
}
