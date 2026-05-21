/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link UserManagementPage}.
 */
public class UserManagementPageAssertions
{
  private final UserManagementPage page;

  public UserManagementPageAssertions(UserManagementPage page) {
    this.page = page;
  }

  public void shouldShowList() {
    assertThat(page.container()).isVisible();
    assertThat(page.userList()).isVisible();
  }

  public void shouldHaveUserCount(int expected) {
    assertThat(page.userListItems()).hasCount(expected);
  }

  public void shouldListUser(String username) {
    assertThat(page.userListItem(username)).isVisible();
  }

  public void shouldNotListUser(String username) {
    assertThat(page.userListItem(username)).hasCount(0);
  }

  public void shouldShowEmptyAddUserForm() {
    assertThat(page.userForm()).isVisible();
    assertThat(page.usernameInput()).hasValue("");
    assertThat(page.firstNameInput()).hasValue("");
    assertThat(page.lastNameInput()).hasValue("");
    assertThat(page.emailInput()).hasValue("");
    assertThat(page.passwordInput()).hasValue("");
    assertThat(page.passwordValidateInput()).hasValue("");
  }
}
