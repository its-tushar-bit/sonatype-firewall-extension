/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class MoveApplicationSuccessModal
    extends BasicElement<MoveApplicationSuccessModal>
{
  public MoveApplicationSuccessModal() {
    super("#success-move-application-modal");
  }

  public SelenideElement infoSection() {
    return child(".nx-modal-content .nx-alert--info");
  }

  public SelenideElement warningSection() {
    return child(".nx-modal-content .nx-alert--warning");
  }

  public SelenideElement okButton() {
    return child(".nx-footer .nx-btn--primary");
  }
}
