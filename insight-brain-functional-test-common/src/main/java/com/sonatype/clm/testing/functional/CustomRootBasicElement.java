/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.codeborne.selenide.SelenideElement;

/**
 * Narrows the search for child elements by providing a
 * root element {@link CustomRootBasicElement.element}
 * instead of a nested selector
 *
 * @param <T>
 */
public class CustomRootBasicElement<T extends BasicElement<T>>
    extends BasicElement<T>
{
  protected CustomRootBasicElement(SelenideElement element) {
    super("");
    this.element = element;
  }

  @Override
  protected SelenideElement child(String... selectors) {
    return this.element.$(childSelector(selectors));
  }
}
