/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MtiqUserManagementPageAssertions
{
  private final MtiqUserManagementPage page;

  public MtiqUserManagementPageAssertions(MtiqUserManagementPage page) {
    this.page = page;
  }

  public void shouldShowPage() {
    assertThat(page.pageTitle()).isVisible();
    assertThat(page.inviteUserButton()).isVisible();
  }

  public void shouldShowLoadErrorContaining(String expectedFragment) {
    assertThat(page.loadError()).containsText(expectedFragment);
  }

  public void shouldShowInviteForm() {
    assertThat(page.inviteFormTitle()).isVisible();
    assertThat(page.inviteForm()).isVisible();
    assertThat(page.inviteFirstNameInput()).isVisible();
    assertThat(page.inviteLastNameInput()).isVisible();
    assertThat(page.inviteEmailInput()).isVisible();
  }

  public void shouldShowValidationErrorContaining(Locator input, String expectedFragment) {
    assertThat(page.validationErrorFor(input)).containsText(expectedFragment);
  }

  public void shouldListUser(String usernameOrDisplayText) {
    assertThat(page.userListItemFor(usernameOrDisplayText)).isVisible();
  }

  /** Callers must first anchor the list load; {@code hasCount(0)} passes on an unloaded list. */
  public void shouldNotListUser(String usernameOrDisplayText) {
    assertThat(page.userListItemFor(usernameOrDisplayText)).hasCount(0);
  }
}
