/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.assertions.LocatorAssertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertions companion for {@link LoginPage}.
 */
public class LoginPageAssertions
{
  private final LoginPage page;

  public LoginPageAssertions(LoginPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.modal()).isVisible();
  }

  /**
   * Assert the modal is visible within the given timeout; useful after logout when the IQ login
   * modal can take several seconds to render.
   */
  public void shouldBeVisibleWithin(long timeoutMs) {
    assertThat(page.modal()).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(timeoutMs));
  }

  public void shouldShowLoginElements() {
    assertThat(page.loginButton()).isVisible();
    assertThat(page.vulnerabilityLookupText()).isVisible();
    assertThat(page.vulnerabilityLookupLink()).isVisible();
  }

  public void shouldBeHidden() {
    assertThat(page.modal()).isHidden();
  }
}
