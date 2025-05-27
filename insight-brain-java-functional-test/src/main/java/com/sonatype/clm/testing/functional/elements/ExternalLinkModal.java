/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class ExternalLinkModal
    extends BasicElement<ExternalLinkModal>
{
  public ExternalLinkModal() {
    super("#external-link-modal");
  }

  public SelenideElement body() {
    return child(".nx-modal-content");
  }

  public SelenideElement closeButton() {
    return child(".nx-btn");
  }
}
