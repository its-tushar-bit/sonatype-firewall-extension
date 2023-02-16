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
  private static final String FOOTER_SELECTOR = ".nx-footer";

  public MoveApplicationDialog() {
    super("#move-application-modal");
  }

  public SelenideElement body() {
    return child(".nx-modal-content");
  }

  public SelenideElement footer() {
    return child(FOOTER_SELECTOR);
  }

  public NxFormSelect destinationDropdown() {
    return new NxFormSelect(childSelector(".nx-form-select"));
  }

  public SelenideElement moveButton() {
    return child(FOOTER_SELECTOR, ".nx-btn--primary");
  }

  public SelenideElement dismissButton() {
    return $(".nx-form__cancel-btn");
  }

  public SelenideElement okButton() {
    return child(".nx-btn--secondary");
  }

  public SelenideElement alertMessage() {
    return $("#move-application-modal .nx-alert__content");
  }

  public SelenideElement errorMessage() {
    return $("#move-application-modal .nx-alert--error .nx-alert__content");
  }

  public SelenideElement incompatibleErrorMessage() {
    return $("#move-application-modal .nx-alert--error b");
  }

  public SelenideElement retryButton() {
    return child(FOOTER_SELECTOR, ".nx-btn--error");
  }
}

