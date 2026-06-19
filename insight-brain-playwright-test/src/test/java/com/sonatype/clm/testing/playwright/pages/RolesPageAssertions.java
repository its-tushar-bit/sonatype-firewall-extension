/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link RolesPage}.
 */
public class RolesPageAssertions
{
  private final RolesPage page;

  public RolesPageAssertions(RolesPage page) {
    this.page = page;
  }

  public void shouldShowContainer() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowPageTitle(String expected) {
    assertThat(page.pageTitle()).hasText(expected);
  }

  public void shouldShowCreateRoleButton() {
    assertThat(page.createRoleButton()).isVisible();
  }

  public void shouldHaveCreateRoleButtonDisabled() {
    assertThat(page.createRoleButton()).isDisabled();
  }

  public void shouldShowBuiltInRoles() {
    assertThat(page.builtInRolesList()).isVisible();
    assertThat(page.builtInRoleItems()).not().hasCount(0);
  }

  public void shouldShowCustomRolesEmpty() {
    assertThat(page.emptyCustomRolesMessage()).isVisible();
  }

  public void shouldListRole(String name) {
    assertThat(page.roleItem(name).first()).isVisible();
  }

  public void shouldNotListRole(String name) {
    assertThat(page.roleItem(name)).hasCount(0);
  }

  public void shouldShowRoleEditor() {
    assertThat(page.roleEditor()).isVisible();
  }

  public void shouldShowRoleEditorTitle(String expected) {
    assertThat(page.roleEditorTitle()).hasText(expected);
  }

  public void shouldShowRoleNameInput() {
    assertThat(page.roleNameInput()).isVisible();
  }

  public void shouldShowRoleDescriptionInput() {
    assertThat(page.roleDescriptionInput()).isVisible();
  }

  public void shouldHaveEmptyRoleNameInput() {
    assertThat(page.roleNameInput()).hasValue("");
  }

  public void shouldHaveEmptyRoleDescriptionInput() {
    assertThat(page.roleDescriptionInput()).hasValue("");
  }

  public void shouldShowPermissionCategory(String displayName) {
    assertThat(page.permissionCategory(displayName)).isVisible();
  }

  public void shouldHavePermissionToggles(String displayName) {
    assertThat(page.permissionToggles(displayName)).not().hasCount(0);
  }

  public void shouldShowRoleFormValidationErrors() {
    assertThat(page.roleEditorFormValidationErrors()).isVisible();
  }

  public void shouldShowRoleFormValidationErrorsContaining(String text) {
    assertThat(page.roleEditorFormValidationErrors()).isVisible();
    assertThat(page.roleEditorFormValidationErrors()).containsText(text);
  }

  public void shouldHaveRoleNameDisabled() {
    assertThat(page.roleNameInput()).isDisabled();
  }

  public void shouldHaveRoleDescriptionDisabled() {
    assertThat(page.roleDescriptionInput()).isDisabled();
  }

  public void shouldHaveSaveButtonDisabled() {
    assertThat(page.roleEditorSaveButton()).isDisabled();
  }

  public void shouldHaveDeleteButtonDisabled() {
    assertThat(page.deleteRoleButton()).isDisabled();
  }

  public void shouldHavePermissionTogglesDisabled(String displayName) {
    assertThat(page.firstPermissionToggle(displayName).locator("input")).isDisabled();
  }

  public void shouldShowPermissionsHeading() {
    assertThat(page.permissionsHeading()).isVisible();
  }

  public void shouldShowRoleDescription(String name) {
    assertThat(page.roleItem(name).first()).isVisible();
    assertThat(page.roleItem(name).first().locator(".nx-list__subtext")).isVisible();
  }

  public void shouldShowDeleteModal() {
    assertThat(page.deleteModal()).isVisible();
  }

  public void shouldShowDeleteWarning() {
    assertThat(page.deleteModalWarning()).isVisible();
  }

  public void shouldShowDeleteWarningContaining(String text) {
    assertThat(page.deleteModalWarning()).containsText(text);
  }
}
