/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class OccurrencesPopover
    extends BasicElement<OccurrencesPopover>
{
  public static final String ROOT = "#occurrences-popover";

  public OccurrencesPopover() {
    super(ROOT);
  }

  public SelenideElement title() {
    return child(".iq-occurrences-popover-header__title-text");
  }

  public SelenideElement closeButton() {
    return child(".iq-occurrences-popover-header__title-close");
  }

  public SelenideElement infoMessage() {
    return child(".iq-occurrences-popover-content__message");
  }

  public SelenideElement externalLink() {
    return child(".nx-text-link--external");
  }

  public SelenideElement subtitle() {
    return child(".nx-h3");
  }
}
