/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;
import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;

public class NxDeleteModal extends BasicElement<NxDeleteModal>
{
  public NxDeleteModal(String selector) {
    super(selector);
  }

  public SelenideElement header() {
    return child(".nx-modal-header");
  }

  public SelenideElement alertContent() {
    return child(".nx-alert__content");
  }

  public static Condition alertText(String username) {
    return text("You are about to permanently remove " + username + ". This action cannot be undone.");
  }

  public SelenideElement closeButton() {
    return child(".nx-form__cancel-btn");
  }

  public SelenideElement submitButton() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement error() {
    return child(".nx-alert--error");
  }
}
