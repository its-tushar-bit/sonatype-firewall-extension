/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MtiqAdministratorsPageAssertions
{
  private final MtiqAdministratorsPage page;

  public MtiqAdministratorsPageAssertions(MtiqAdministratorsPage page) {
    this.page = page;
  }

  public void shouldShowListPageLayout() {
    assertThat(page.listContainer()).isVisible();
    assertThat(page.listPageHeading()).isVisible();
    assertThat(page.configureAdministratorsHeading()).isVisible();
    assertThat(page.rolesTable()).isVisible();
  }

  public void shouldShowRoleRow(String roleName) {
    assertThat(page.roleRowByName(roleName)).isVisible();
  }

  public void shouldShowRoleMembers(String roleName, String membersSubstring) {
    assertThat(page.roleRowMembersText(roleName)).containsText(membersSubstring);
  }

  public void shouldHideLocalUsernameAndPasswordInputs() {
    assertThat(page.localUsernameInput()).hasCount(0);
    assertThat(page.localPasswordInput()).hasCount(0);
  }

  public void shouldShowEditPageLayout() {
    assertThat(page.editContainer()).isVisible();
    assertThat(page.editPageHeading()).isVisible();
    assertThat(page.addMembersHeading()).isVisible();
    assertThat(page.addMembersForm()).isVisible();
  }

  public void shouldShowSearchMatchInDropdown(String displayText) {
    assertThat(page.searchMatchOption(displayText)).isVisible();
  }

  public void shouldShowAssociatedMember(String displayText) {
    assertThat(page.associatedMemberItem(displayText)).isVisible();
  }

  public void shouldEnableSubmitButton() {
    assertThat(page.submitButton()).isEnabled();
  }
}
