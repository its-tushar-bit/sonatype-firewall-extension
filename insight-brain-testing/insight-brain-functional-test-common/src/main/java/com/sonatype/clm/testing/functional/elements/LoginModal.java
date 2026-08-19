/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LoginModal
    extends BasicElement<LoginModal>
{
  public LoginModal() {
    super("#iq-login-modal");
  }

  public SelenideElement ssoButton() {
    return $("#iq-login-modal-sso-button");
  }

  public SelenideElement username() {
    return $("#iq-login-modal-username-input");
  }

  public SelenideElement password() {
    return $("#iq-login-modal-password-input");
  }

  public SelenideElement loginButton() {
    return $(".nx-form__submit-btn");
  }

  public SelenideElement cancelButton() {
    return $(".nx-form__cancel-btn");
  }

  public SelenideElement systemNotice() {
    return $(".iq-login-modal-system-notice");
  }

  public SelenideElement errorMessage() {
    return $(".nx-load-error__message");
  }

  public SelenideElement header() {
    return $(".nx-modal-header");
  }

  public SelenideElement vulnerabilityLookupText() {
    return $(".iq-login-modal-helper-text");
  }

  public SelenideElement vulnerabilityLookupLink() {
    return $(".iq-login-modal-helper-text a");
  }
}
