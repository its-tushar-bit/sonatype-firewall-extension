/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class RevokeLegacyViolationModal
    extends BasicElement<RevokeLegacyViolationModal>
{
  private static final String FOOTER_SELECTOR = ".nx-footer";

  public RevokeLegacyViolationModal() {
    super("#revoke-legacy-violation-modal");
  }

  public SelenideElement header() {
    return child(".nx-modal-header");
  }

  public SelenideElement body() {
    return child(".nx-modal-content");
  }

  public SelenideElement footer() {
    return child(FOOTER_SELECTOR);
  }

  public SelenideElement revokeButton() {
    return child(FOOTER_SELECTOR, ".nx-form__submit-btn");
  }

  public SelenideElement retryButton() {
    return child(FOOTER_SELECTOR, ".nx-load-error__retry");
  }

  public SelenideElement cancelButton() {
    return child(FOOTER_SELECTOR, ".nx-form__cancel-btn");
  }
}
