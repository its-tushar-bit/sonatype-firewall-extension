/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class NxCodeSnippet
    extends BasicElement<NxCodeSnippet>
{
  public NxCodeSnippet(String selector) {
    super(selector);
  }

  public SelenideElement copyToClipboard() {
    return child(".nx-btn");
  }

  public SelenideElement label() {
    return child(".nx-label__text");
  }

  public SelenideElement content() {
    return child(".nx-text-input__input");
  }
}
