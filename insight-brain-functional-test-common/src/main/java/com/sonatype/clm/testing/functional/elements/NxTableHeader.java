/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class NxTableHeader
    extends BasicElement<NxTableHeader>
{
  public NxTableHeader(String selector) {
    super(selector);
  }

  public SelenideElement sortBtn() {
    return child(".nx-cell__sort-btn");
  }
}
