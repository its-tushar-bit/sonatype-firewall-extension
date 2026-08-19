/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link LdapPage}.
 */
public class LdapPageAssertions
{
  private final LdapPage page;

  public LdapPageAssertions(LdapPage page) {
    this.page = page;
  }

  public void shouldShowListHeading() {
    assertThat(page.listPageHeading()).isVisible();
  }

  public void shouldShowTileHeading() {
    assertThat(page.tileHeading()).isVisible();
  }

  public void shouldShowAddServerButton() {
    assertThat(page.addServerButton()).isVisible();
  }

  public void shouldShowServerList() {
    assertThat(page.serverList()).isVisible();
  }

  public void shouldShowEmptyListMessage() {
    assertThat(page.emptyListMessage()).isVisible();
  }

  public void shouldHaveServerCount(int expected) {
    assertThat(page.serverListItems()).hasCount(expected);
  }

  public void shouldShowCreateContainer() {
    assertThat(page.createContainer()).isVisible();
  }

  public void shouldShowCreateHeading() {
    assertThat(page.createPageHeading()).isVisible();
  }

  public void shouldShowCreateForm() {
    assertThat(page.createForm()).isVisible();
  }

  public void shouldShowServerNameInput() {
    assertThat(page.serverNameInput()).isVisible();
  }

  public void shouldShowEditorContainer() {
    assertThat(page.editorContainer()).isVisible();
  }

  public void shouldShowEditorHeading() {
    assertThat(page.editorPageHeading()).isVisible();
  }

  public void shouldShowHostnameInput() {
    assertThat(page.hostnameInput()).isVisible();
  }

  public void shouldShowPortInput() {
    assertThat(page.portInput()).isVisible();
  }

  public void shouldShowSearchBaseInput() {
    assertThat(page.searchBaseInput()).isVisible();
  }

  public void shouldShowProtocolSelector() {
    assertThat(page.protocolSelector()).isVisible();
  }

  public void shouldShowAuthMethodSelector() {
    assertThat(page.authMethodSelector()).isVisible();
  }

  public void shouldShowTestConnectionButton() {
    assertThat(page.testConnectionButton()).isVisible();
  }

  public void shouldShowRemoveServerButton() {
    assertThat(page.removeServerButton()).isVisible();
  }

  public void shouldShowUserMappingTab() {
    assertThat(page.userMappingTab()).isVisible();
  }

  public void shouldShowUserBaseDnInput() {
    assertThat(page.userBaseDnInput()).isVisible();
  }

  public void shouldShowUserObjectClassInput() {
    assertThat(page.userObjectClassInput()).isVisible();
  }

  public void shouldShowUserIdAttributeInput() {
    assertThat(page.userIdAttributeInput()).isVisible();
  }

  public void shouldShowUserRealNameAttributeInput() {
    assertThat(page.userRealNameAttributeInput()).isVisible();
  }

  public void shouldShowUserEmailAttributeInput() {
    assertThat(page.userEmailAttributeInput()).isVisible();
  }

  public void shouldShowGroupMappingTypeSelector() {
    assertThat(page.groupMappingTypeSelector()).isVisible();
  }

  public void shouldShowRemoveModal() {
    assertThat(page.removeModal()).isVisible();
  }

  public void shouldShowRemoveModalHeading() {
    assertThat(page.removeModalHeading()).isVisible();
  }

  public void shouldShowRemoveModalWarning() {
    assertThat(page.removeModalWarning()).isVisible();
  }

  public void shouldShowRemoveModalDeleteButton() {
    assertThat(page.removeModalDeleteButton()).isVisible();
  }

  public void shouldShowRemoveModalCancelButton() {
    assertThat(page.removeModalCancelButton()).isVisible();
  }
}
