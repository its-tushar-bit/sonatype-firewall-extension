/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class ChangeApplicationIdDialog
    extends BasicElement<ChangeApplicationIdDialog>
{
  private static final String FOOTER_SELECTOR = ".nx-footer";

  public ChangeApplicationIdDialog() {
    super("#change-application-id-modal");
  }

  public SelenideElement body() {
    return child(".nx-modal-content");
  }

  public SelenideElement currentId() {
    return child(".nx-read-only > .nx-read-only__data");
  }

  public SelenideElement newIdDiv() {
    return child("#editor-new-id .nx-text-input");
  }

  public SelenideElement newId() {
    return child("#editor-new-id .nx-text-input .nx-text-input__input");
  }

  public SelenideElement newIdInvalidMessage() {
    return child("#editor-new-id > .nx-text-input > .nx-field-validation-message");
  }

  public SelenideElement changeButton() {
    return child(FOOTER_SELECTOR, ".nx-btn.nx-btn--primary");
  }
}
