/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class AgeInput
{
  private String rootSelector;

  public AgeInput(String rootSelector) {
    this.rootSelector = rootSelector;
  }

  public SelenideElement age() {
    return $(rootSelector + " .iq-text-input--age-input");
  }

  public Dropdown modifier() {
    return new Dropdown(rootSelector, "dropdown-selector");
  }
}
