/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;

import com.codeborne.selenide.SelenideElement;

public class PillButton
    extends BasicElement<PillButton>
{
  private final SelenideElement scrollContainer;

  public PillButton(SelenideElement scrollContainer, String selector) {
    super(selector);
    this.scrollContainer = scrollContainer;
  }

  @Override
  public void click() {
    scrollContainer.shouldHave(ScrollUtil.scrollSpyInitialized);
    super.click();
    ScrollUtil.awaitEndOfScrolling(scrollContainer.find(":first-child"));
  }
}
