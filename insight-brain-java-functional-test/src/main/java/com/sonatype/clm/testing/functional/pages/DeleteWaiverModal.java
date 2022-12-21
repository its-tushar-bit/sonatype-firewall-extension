/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class DeleteWaiverModal
    extends BasicElement<DeleteWaiverModal>
{
  private static final String ROOT_SELECTOR = "#delete-waiver-modal";

  public SelenideElement root() {
    return $(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child(".nx-modal-header");
  }

  public SelenideElement message() {
    return child(".nx-modal-content");
  }

  public SelenideElement cancelButton() {
    return child("#delete-waiver-modal-cancel-button");
  }

  public SelenideElement yesButton() {
    return child("#delete-waiver-modal-continue-button");
  }
}
