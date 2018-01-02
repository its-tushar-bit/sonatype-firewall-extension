/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ChangePasswordModal
    extends BasicElement<ChangePasswordModal>
{
  public ChangePasswordModal() {
    super("#change-password-modal");
  }

  public SelenideElement oldPassword() {
    return $("#original-password");
  }

  public SelenideElement newPassword() {
    return $("#new-password");
  }

  public SelenideElement newPasswordValidate() {
    return $("#confirm-password");
  }

  public SelenideElement invalidCredentialsError() {
    return $("#change-password-error");
  }

  public SelenideElement ok() {
    return $("#change-password-submit");
  }

  public SelenideElement cancel() {
    return $("#change-password-cancel");
  }
}
