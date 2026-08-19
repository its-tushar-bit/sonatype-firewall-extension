/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class DeleteAnnotationModal
    extends BasicElement<DeleteAnnotationModal>
{
  static final String ROOT_SELECTOR = "#delete-vex-annotation-modal";

  public DeleteAnnotationModal() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child(".nx-modal-header");
  }

  public SelenideElement body() {
    return child(".nx-modal-content");
  }

  public SelenideElement successModal() {
    return child(".nx-submit-mask__message");
  }

  public SelenideElement cancelButton() {
    return child(".nx-form__cancel-btn");
  }

  public SelenideElement submitButton() {
    return child(".nx-form__submit-btn");
  }
}
