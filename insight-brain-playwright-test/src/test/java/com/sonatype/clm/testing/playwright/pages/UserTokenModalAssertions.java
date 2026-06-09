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
 * Assertions companion for {@link UserTokenModal}.
 */
public class UserTokenModalAssertions
{
  private final UserTokenModal page;

  public UserTokenModalAssertions(UserTokenModal page) {
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

  public void shouldShowInitialState() {
    assertThat(page.modal())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(page.generateUserTokenButton())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
    assertThat(page.tokenExistenceAlert()).isHidden();
    assertThat(page.deleteUserTokenButton()).isHidden();
    assertThat(page.userCodeInput()).isHidden();
    assertThat(page.passCodeInput()).isHidden();
  }

  public void shouldShowGeneratedCredentials() {
    assertThat(page.userCodeInput()).isVisible();
    assertThat(page.passCodeInput()).isVisible();
    assertThat(page.generateUserTokenButton()).isHidden();
    assertThat(page.deleteUserTokenButton()).isHidden();
  }

  public void shouldShowExistingTokenState() {
    assertThat(page.modal())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(page.deleteUserTokenButton()).isVisible();
    assertThat(page.tokenExistenceAlert()).isVisible();
    assertThat(page.deleteUserTokenButton()).isVisible();
    assertThat(page.generateUserTokenButton()).isHidden();
    assertThat(page.userCodeInput()).isHidden();
    assertThat(page.passCodeInput()).isHidden();
  }

  public void shouldShowExpirationBlock(String expectedHeading, String expectedSubtitle, String expectedDatePrefix) {
    assertThat(page.expirationSection()).isVisible();
    assertThat(page.expirationHeading()).isVisible();
    assertThat(page.expirationHeading()).containsText(expectedHeading);
    assertThat(page.expirationSubtitle()).isVisible();
    assertThat(page.expirationSubtitle()).containsText(expectedSubtitle);
    assertThat(page.expirationDate()).isVisible();
    assertThat(page.expirationDate()).containsText(expectedDatePrefix);
  }
}
