/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LoginDialog
{

  public static SelenideElement root() {
    return $("#login-modal");
  }

  public static SelenideElement username() {
    return $("#login-username");
  }

  public static SelenideElement password() {
    return $("#login-password");
  }

  public static SelenideElement loginButton() {
    return $("#login-action");
  }
}
