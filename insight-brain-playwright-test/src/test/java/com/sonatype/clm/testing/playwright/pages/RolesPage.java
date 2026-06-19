/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Roles management page (Administration → Roles).
 * Covers the roles list, Create Role form, Edit Role form, and Delete Role modal.
 */
public class RolesPage
    extends BasePage
{
  private static final String ROOT = "#role-management";

  private static final String EDITOR = "#role-editor";

  private static final Locator.GetByRoleOptions CREATE_ROLE_BUTTON_OPTIONS =
      new Locator.GetByRoleOptions().setName("Create Role");

  private static final Pattern ROLES_LIST_URL = Pattern.compile(".*/roles(\\?.*)?$");

  public RolesPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/roles";
  }

  public static String urlToCreateRole() {
    return "/assets/index.html#/roles/_new_";
  }

  public static String urlToEditRole(String roleId) {
    return "/assets/index.html#/roles/" + roleId;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageTitle() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Roles").setExact(true));
  }

  public Locator createRoleButton() {
    return container().getByRole(AriaRole.BUTTON, CREATE_ROLE_BUTTON_OPTIONS);
  }

  public Locator builtInRolesList() {
    return locator("#builtin-roles");
  }

  public Locator customRolesList() {
    return locator("#custom-roles");
  }

  public Locator builtInRoleItems() {
    return builtInRolesList().getByRole(AriaRole.LISTITEM);
  }

  public Locator customRoleItems() {
    return customRolesList().getByRole(AriaRole.LISTITEM);
  }

  public Locator roleItem(String name) {
    return container().getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText(name));
  }

  public Locator emptyCustomRolesMessage() {
    return customRolesList().getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText("No custom roles defined"));
  }

  public Locator roleEditor() {
    return locator(EDITOR);
  }

  public Locator roleEditorTitle() {
    return roleEditor().getByRole(AriaRole.HEADING).first();
  }

  public Locator roleNameInput() {
    return roleEditor().getByLabel("Role Name");
  }

  public Locator roleDescriptionInput() {
    return roleEditor().getByLabel("Role Description");
  }

  public Locator roleEditorForm() {
    return roleEditor().locator(".nx-form");
  }

  public Locator roleEditorSaveButton() {
    return roleEditor().getByRole(AriaRole.BUTTON, CommonButtonOptions.SAVE_BUTTON_OPTS);
  }

  public Locator roleEditorCancelButton() {
    return roleEditor().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator roleEditorFormValidationErrors() {
    return roleEditor().locator(".nx-form__validation-errors");
  }

  public Locator permissionsHeading() {
    return roleEditor().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Permissions"));
  }

  public Locator permissionCategory(String displayName) {
    return roleEditor().locator("section.nx-tile-subsection")
        .filter(new Locator.FilterOptions()
            .setHas(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(displayName).setExact(true).setLevel(3))));
  }

  public Locator permissionToggles(String displayName) {
    return permissionCategory(displayName).locator(".nx-toggle");
  }

  public Locator firstPermissionToggle(String displayName) {
    return permissionToggles(displayName).first();
  }

  public Locator deleteRoleButton() {
    return roleEditor().getByRole(AriaRole.BUTTON, CommonButtonOptions.DELETE_BUTTON_OPTS);
  }

  public Locator deleteModal() {
    return byRole(AriaRole.DIALOG);
  }

  public Locator deleteModalSubmit() {
    return deleteModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.DELETE_BUTTON_OPTS);
  }

  public Locator deleteModalCancel() {
    return deleteModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator deleteModalWarning() {
    return deleteModal().locator(".nx-alert--warning");
  }

  public void clickCreateRole() {
    createRoleButton().click();
    assertThat(roleEditor()).isVisible();
  }

  public void fillRoleDetails(String name, String description) {
    roleNameInput().fill(name);
    roleDescriptionInput().fill(description);
  }

  public void clickSave() {
    roleEditorSaveButton().click();
  }

  public void clickCancel() {
    roleEditorCancelButton().click();
  }

  public void openDeleteModal() {
    deleteRoleButton().click();
    assertThat(deleteModal()).isVisible();
  }

  public void confirmDelete() {
    deleteModalSubmit().click();
  }

  public void openRoleForEdit(String roleName) {
    roleItem(roleName).first().click();
    assertThat(roleEditor()).isVisible();
  }

  public void waitForRolesListRoute() {
    assertThat(page).hasURL(ROLES_LIST_URL);
  }
}
