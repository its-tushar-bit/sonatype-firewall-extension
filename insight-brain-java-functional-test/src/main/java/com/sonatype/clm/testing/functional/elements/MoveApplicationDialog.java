/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class MoveApplicationDialog
    extends BasicElement<MoveApplicationDialog>
{
  private static final String FOOTER_SELECTOR = ".iq-modal-footer";

  public MoveApplicationDialog() {
    super("#move-app-modal");
  }

  public SelenideElement body() {
    return child(".iq-modal-content");
  }

  public SelenideElement footer() {
    return child(FOOTER_SELECTOR);
  }

  public Dropdown destinationDropdown() {
    return new Dropdown("#select-parent-organization");
  }

  public SelenideElement moveButton() {
    return child(FOOTER_SELECTOR, ".iq-btn--primary");
  }

  public SelenideElement dismissButton() {
    return $("#dismiss-btn");
  }

  public SelenideElement detailsButton() {
    return child(FOOTER_SELECTOR, ".iq-btn--tertiary");
  }

  public SelenideElement retryButton() {
    return child(FOOTER_SELECTOR, ".iq-btn--error");
  }
}

