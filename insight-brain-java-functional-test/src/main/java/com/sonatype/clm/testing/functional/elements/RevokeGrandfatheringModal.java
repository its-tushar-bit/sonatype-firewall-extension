/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class RevokeGrandfatheringModal
    extends BasicElement<RevokeGrandfatheringModal>
{
  private static final String FOOTER_SELECTOR = ".iq-modal-footer";

  public RevokeGrandfatheringModal() {
    super("#revoke-grandfathering-modal");
  }

  public SelenideElement body() {
    return child(".iq-modal-content");
  }

  public SelenideElement footer() {
    return child(FOOTER_SELECTOR);
  }

  public SelenideElement revokeButton() {
    return child(FOOTER_SELECTOR, ".iq-btn.iq-btn--primary");
  }

  public SelenideElement retryButton() {
    return child(FOOTER_SELECTOR, ".iq-btn.iq-btn--error");
  }

  public SelenideElement cancelButton() {
    return child(FOOTER_SELECTOR, ".iq-btn.iq-btn--cancel");
  }
}

