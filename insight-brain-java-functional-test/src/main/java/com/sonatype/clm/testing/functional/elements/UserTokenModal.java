/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class UserTokenModal
    extends BasicElement<UserTokenModal>
{
  public UserTokenModal() {
    super("#user-token-modal");
  }

  public SelenideElement deleteUserTokenButton() {
    return child("#delete-user-token");
  }

  public SelenideElement generateUserTokenButton() {
    return child("#generate-user-token");
  }

  public SelenideElement cancelButton() {
    return child("#user-token-modal-cancel");
  }

  public SelenideElement tokenExistenceAlert() {
    return child("#user-token-modal-token-exists-alert");
  }

  public SelenideElement userCodeInput() {
    return child("#user-token-usercode");
  }

  public SelenideElement passCodeInput() {
    return child("#user-token-passcode");
  }

  public SelenideElement expirationSection() {
    return child(".iq-user-token-expiration");
  }

  public SelenideElement expirationHeading() {
    return child(".iq-user-token-expiration__heading");
  }

  public SelenideElement expirationSubtitle() {
    return child(".iq-user-token-expiration__subtitle");
  }

  public SelenideElement expirationDate() {
    return child(".iq-user-token-expiration__date");
  }
}
