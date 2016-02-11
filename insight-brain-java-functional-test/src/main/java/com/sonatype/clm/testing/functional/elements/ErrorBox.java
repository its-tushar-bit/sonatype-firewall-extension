/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ErrorBox
    extends BasicElement<ErrorBox>
{
  public ErrorBox(String... selector) {
    super(selector);
  }

  public SelenideElement message() {
    return $(SelectorUtils.selector(selector, "div:first-child"));
  }

  public SelenideElement retryButton() {
    return $(SelectorUtils.selector(selector, "button"));
  }
}
