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
    super("#move-app-success-modal");
  }

  public SelenideElement infoSection() {
    return child(".clm-modal-body .clm-alert.alert-info");
  }

  public SelenideElement warningSection() {
    return child(".clm-modal-body .clm-alert.alert");
  }

  public SelenideElement okButton() {
    return child(".clm-modal-footer .btn-primary");
  }
}
