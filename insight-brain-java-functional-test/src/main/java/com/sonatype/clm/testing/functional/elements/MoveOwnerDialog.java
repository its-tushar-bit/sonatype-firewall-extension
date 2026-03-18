/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class MoveOwnerDialog
    extends BasicElement<MoveOwnerDialog>
{
  private static final String FOOTER_SELECTOR = ".nx-footer";

  private static final String BTN_BAR_SELECTOR = ".nx-btn-bar";

  public MoveOwnerDialog() {
    super("#move-owner-modal");
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

  public SelenideElement header() {
    return child(".nx-modal-header");
  }

  public SelenideElement message() {
    return child("#move-modal-info-message");
  }

  public SelenideElement dismissButton() {
    return $(".nx-form__cancel-btn");
  }

  public SelenideElement okButton() {
    return child(".nx-btn--secondary");
  }

  public SelenideElement errorMessage() {
    return $("#move-owner-modal .nx-alert--error .nx-alert__content");
  }

  public SelenideElement incompatibleErrorMessage() {
    return $("#move-owner-modal .nx-alert--error b");
  }

  public SelenideElement retryButton() {
    return child(FOOTER_SELECTOR, ".nx-btn--error");
  }

  public SelenideElement fetchCSVButton() {
    return child(FOOTER_SELECTOR, BTN_BAR_SELECTOR, ".nx-btn--tertiary");
  }
}
