/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class ResetPasswordModal
    extends BasicElement<ResetPasswordModal>
{
  public ResetPasswordModal() {
    super("#reset-password-modal");
  }

  public SelenideElement reset() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child(".nx-form__cancel-btn");
  }
}
