/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class AgeInput
    extends BasicElement<AgeInput>
{
  private String rootSelector;

  public AgeInput(String rootSelector) {
    this.rootSelector = rootSelector;
  }

  public SelenideElement age() {
    return $(rootSelector + " .constraint-editor__age-input input");
  }

  public NxFormSelect modifier() {
    return new NxFormSelect(childSelector(rootSelector + " .constraint-editor__age-modifier"));
  }
}
