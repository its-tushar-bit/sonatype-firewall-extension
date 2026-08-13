/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link ChangePasswordModal}.
 */
public class ChangePasswordModalAssertions
{
  private final ChangePasswordModal page;

  public ChangePasswordModalAssertions(ChangePasswordModal page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.modal())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void shouldBeHidden() {
    assertThat(page.modal())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void shouldShowPasswordMismatchValidation(String expectedMessage) {
    assertThat(page.formValidationErrors().first()).isVisible();
    assertThat(page.formValidationErrors().first()).containsText(expectedMessage);
  }

  public void shouldShowInvalidCredentialsError() {
    assertThat(page.invalidCredentialsError()).isVisible();
  }
}
