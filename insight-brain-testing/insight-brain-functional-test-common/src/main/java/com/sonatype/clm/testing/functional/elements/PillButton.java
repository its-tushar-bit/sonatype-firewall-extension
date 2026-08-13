/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class PillButton
    extends BasicElement<PillButton>
{
  public PillButton(String selector) {
    super(selector);
  }

  @Override
  public PillButton click() {
    // There is a race condition with the initialization of the pill bar that can sometimes cause the scrolling to
    // not work as expected. A short sleep here fixes it.
    Selenide.sleep(1000L);

    SelenideElement currentPill = $(selector);
    currentPill.scrollIntoView("{inline: 'center'}");
    ScrollUtil.awaitEndOfScrolling(currentPill);
    SelenideElement scrollTarget = $("#" + getElement().data("scroll"));
    super.click();
    ScrollUtil.awaitEndOfScrolling(scrollTarget);
    return me();
  }
}
