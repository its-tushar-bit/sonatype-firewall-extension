/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class NxTextInput
    extends BasicElement<NxTextInput>
{
  private SelenideElement element;

  public NxTextInput(SelenideElement element) {
    this.element = element;
  }

  public SelenideElement input() {
    return this.element;
  }

  public SelenideElement inputWrapper() {
    return this.element.closest(".nx-text-input");
  }

  public SelenideElement errorMessage() {
    return this.inputWrapper().find(".nx-field-validation-message");
  }
}
