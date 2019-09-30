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
    super("#login-modal");
  }

  public SelenideElement ssoText() {
    return $("#sso-text");
  }

  public SelenideElement username() {
    return $("#login-username");
  }

  public SelenideElement password() {
    return $("#login-password");
  }

  public SelenideElement loginButton() {
    return $("#login-action");
  }

  public SelenideElement cancelButton() {
    return $("#login-cancel");
  }
  
  public SelenideElement ssoButton() {
    return $("#sso-action");
  }

  public SelenideElement systemNotice() {
    return child("system-notice div");
  }

  public SelenideElement errorMessage() {
    return $("#login-error");
  }

  public SelenideElement header() {
    return $(".iq-modal-header");
  }

  public SelenideElement vulnerabilityLookupText() {
    return $("#login-vulnerability-link");
  }

  public SelenideElement vulnerabilityLookupLink() {
    return $("#login-vulnerability-link a");
  }
}
