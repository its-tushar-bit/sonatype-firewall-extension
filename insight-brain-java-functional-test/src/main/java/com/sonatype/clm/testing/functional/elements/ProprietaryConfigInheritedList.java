/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class ProprietaryConfigInheritedList
    extends BasicElement<ProprietaryConfigInheritedList>
{
  public ProprietaryConfigInheritedList(String... selectors) {
    super(selectors);
  }

  public SelenideElement ownerName() {
    return child(".nx-h3");
  }

  public ProprietaryComponentMatcher inheritedMatcher(ProprietaryComponentMatcher.MatcherType type, String name) {
    return new ProprietaryComponentMatcher(selector, type, name);
  }

  public ElementsCollection inheritedMatchers() {
    return children("ul", ".nx-list__item");
  }
}
