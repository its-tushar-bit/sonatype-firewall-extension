/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxToggle;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class UserTokensConfigurationPage
    extends BasicElement<UserTokensConfigurationPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/userTokensConfiguration");
  }

  private static final String ROOT_SELECTOR = "#user-tokens-configuration";

  public UserTokensConfigurationPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement pageTitle() {
    return child(".nx-page-title h1");
  }

  public SelenideElement pageDescription() {
    return child(".nx-page-title__description p");
  }

  public SelenideElement tileHeaderTitle() {
    return child(".nx-tile-header__title h2");
  }

  public SelenideElement explanation() {
    return child(".nx-tile-content > p");
  }

  public NxToggle userTokensEnabledToggle() {
    return new NxToggle(childSelector("#user-tokens-enabled-toggle"));
  }

  public NxToggle expirationEnabledToggle() {
    return new NxToggle(childSelector("#user-token-expiration-toggle"));
  }

  public SelenideElement expirationDaysInput() {
    return child("#user-token-expiry-days");
  }

  public SelenideElement validationError() {
    return child("#user-token-expiry-days").closest(".nx-text-input").find(".nx-field-validation-message");
  }

  public SelenideElement update() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child("#user-tokens-cancel");
  }

  public SelenideElement manageUserTokenLink() {
    return child(".nx-tile-content > p .nx-text-link");
  }

  public SelenideElement modal() {
    return child(".nx-modal");
  }
}
