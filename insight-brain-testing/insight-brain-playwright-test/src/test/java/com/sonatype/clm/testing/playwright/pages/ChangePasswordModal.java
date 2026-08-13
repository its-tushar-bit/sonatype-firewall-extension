/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Change Password modal dialog.
 */
public class ChangePasswordModal
    extends BasePage
{
  private static final String ROOT = "#change-password-modal";

  public ChangePasswordModal() {
    super();
  }

  public Locator modal() {
    // NxModal does not set aria-labelledby in this RSC version, so the dialog element has no
    // accessible name and getByRole(DIALOG,"Change Password") never resolves. Use the stable id.
    return locator(ROOT);
  }

  public Locator oldPassword() {
    return locator(ROOT + " #original-password");
  }

  public Locator newPassword() {
    return locator(ROOT + " #new-password");
  }

  public Locator newPasswordValidate() {
    return locator(ROOT + " #confirm-password");
  }

  public Locator okButton() {
    return locator(ROOT + " .nx-form__submit-btn");
  }

  public Locator cancelButton() {
    return locator(ROOT + " .nx-form__cancel-btn");
  }

  public Locator invalidCredentialsError() {
    return locator(ROOT + " #change-password-error");
  }

  public Locator formValidationErrors() {
    return locator(ROOT + " .nx-field-validation-message");
  }

  public void fillPasswords(String oldPassword, String newPassword, String confirmation) {
    oldPassword().fill(oldPassword);
    newPassword().fill(newPassword);
    newPasswordValidate().fill(confirmation);
  }

  public void submit() {
    assertThat(okButton()).isEnabled();
    okButton().click();
  }

}
