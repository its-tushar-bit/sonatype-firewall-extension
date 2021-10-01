/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class SimilarMatchesPopover
    extends BasicElement<SimilarMatchesPopover>
{
  public static final String ROOT = "#similar-matches-popover";

  public SimilarMatchesPopover() {
    super(ROOT);
  }

  public SelenideElement title() {
    return child(".iq-popover-header__title-text");
  }

  public SelenideElement closeButton() {
    return child(".iq-popover-header__close-btn");
  }

  public SelenideElement componentIdentificationInformation() {
    return child(".iq-similar-matches-popover-content__message");
  }

  public SelenideElement bestMatchSubtitle() {
    return child(".nx-h3");
  }

  public SelenideElement bestMatchListItem() {
    return child(".iq-similar-match");
  }
}
