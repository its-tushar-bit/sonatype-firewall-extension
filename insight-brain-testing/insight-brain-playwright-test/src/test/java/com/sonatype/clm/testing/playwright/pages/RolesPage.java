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
import com.microsoft.playwright.options.WaitForSelectorState;

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

  /**
   * Playwright serializes to JS RegExp — no {@code \Q…\E}, no {@code (?i)}; use {@link BasePage#escapeForJsRegex} for
   * dynamic input.
   */
  private static final Pattern ROLES_LIST_URL = Pattern.compile(".*/roles(\\?.*)?$");

  public RolesPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/roles";
  }

  /** Roles list accessed from Firewall context. Matches ui-router state {@code firewall.rolesList}. */
  public static String firewallUrl() {
    return "/assets/index.html#/firewall/roles";
  }

  public static String urlToCreateRole() {
    return "/assets/index.html#/roles/_new_";
  }

  public static String urlToEditRole(String roleId) {
    return "/assets/index.html#/roles/" + roleId;
  }

  public static Pattern editRoleUrlPattern(String roleId) {
    return Pattern.compile(".*/roles/" + escapeForJsRegex(roleId) + "$");
  }

  /** Id-anchored: list and editor both render {@code <main>} on different routes. */
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

  /** Load-error alert shown when the roles-list fetch fails. */
  public Locator loadError() {
    return nxLoadErrorAlert(container());
  }

  public Locator retryButton() {
    return nxLoadErrorRetryButton(container());
  }

  /**
   * Two unlabelled sibling {@code
   *
  <ul>
   * }s — id is the only stable hook (RoleList.jsx:57,63).
   */
  public Locator builtInRolesList() {
    return locator("#builtin-roles");
  }

  public Locator customRolesList() {
    return locator("#custom-roles");
  }

  public Locator builtInRoleItems() {
    return builtInRolesList().getByRole(AriaRole.LISTITEM);
  }

  /** Exact-text match on the {@code nx-list__text} span — the row's anchor concatenates name + description. */
  public Locator roleItem(String name) {
    return container().getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHas(
            page.getByText(name, new Page.GetByTextOptions().setExact(true))));
  }

  public Locator emptyCustomRolesMessage() {
    return customRolesList().getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText("No custom roles defined"));
  }

  /** Separate {@code <main>} from {@link #container()} — see that locator's note. */
  public Locator roleEditor() {
    return locator(EDITOR);
  }

  /**
   * The editor title is the only h1 in {@link #roleEditor()}; the "Permissions" section heading is
   * a lower-level heading. Pinning to {@code setLevel(1)} fails loudly if a future change adds a
   * sibling h1, instead of silently asserting against the wrong element.
   */
  public Locator roleEditorTitle() {
    return roleEditor().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  public Locator roleNameInput() {
    return roleEditor().getByLabel("Role Name");
  }

  public Locator roleDescriptionInput() {
    return roleEditor().getByLabel("Role Description");
  }

  public Locator roleEditorSaveButton() {
    return roleEditor().getByRole(AriaRole.BUTTON, CommonButtonOptions.SAVE_BUTTON_OPTS);
  }

  public Locator roleEditorCancelButton() {
    return roleEditor().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator roleEditorFormValidationErrors() {
    return roleEditor().getByLabel("form validation errors");
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

  /** Outer label only — assert disabled state via {@code .locator("input").isDisabled()}, not {@code hasClass}. */
  public Locator permissionToggles(String displayName) {
    return permissionCategory(displayName).locator(".nx-toggle");
  }

  public Locator firstPermissionToggle(String displayName) {
    return permissionToggles(displayName).first();
  }

  public Locator deleteRoleButton() {
    return roleEditor().getByRole(AriaRole.BUTTON, CommonButtonOptions.DELETE_BUTTON_OPTS);
  }

  /**
   * Anchored on the dialog's "Delete Role" {@code h2} (RoleEditor.jsx:180-183) so any future
   * concurrent dialog cannot satisfy this locator. {@code NxModal} does not set an
   * {@code aria-label}/{@code aria-labelledby}, so a {@code byRole(DIALOG, "Delete Role")} would
   * not work — the inner heading text is not promoted into the dialog's accessible name.
   */
  public Locator deleteModal() {
    return byRole(AriaRole.DIALOG).filter(new Locator.FilterOptions()
        .setHas(page.getByRole(AriaRole.HEADING,
            new Page.GetByRoleOptions().setLevel(2).setName("Delete Role"))));
  }

  public Locator deleteModalSubmit() {
    return deleteModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.DELETE_BUTTON_OPTS);
  }

  public Locator deleteModalCancel() {
    return deleteModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  /** RSC's NxAlert wrapper has no role/name — CSS class is the only hook. */
  public Locator deleteModalWarning() {
    return deleteModal().locator(".nx-alert--warning");
  }

  /** Same as {@link #deleteModalWarning()} — NxAlert wrapper has no role. */
  public Locator infoAlert() {
    return container().locator(".nx-alert--info");
  }

  public Locator infoAlertDocsLink() {
    return infoAlert().getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Docs"));
  }

  public Locator builtInSubtitle() {
    return container().getByText("Built-In", new Locator.GetByTextOptions().setExact(true));
  }

  public Locator customSubtitle() {
    return container().getByText("Custom", new Locator.GetByTextOptions().setExact(true));
  }

  public Locator roleItemAnchor(Locator roleItem) {
    return roleItem.getByRole(AriaRole.LINK).first();
  }

  public void clickCreateRole() {
    createRoleButton().click();
    roleEditor().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
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
    deleteModal().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
  }

  public void confirmDelete() {
    deleteModalSubmit().click();
  }

  /** Asserts exactly one match before click so duplicate role names fail loudly, not silently. */
  public void openRoleForEdit(String roleName) {
    assertThat(roleItem(roleName)).hasCount(1);
    roleItem(roleName).click();
    roleEditor().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
  }

  public void waitForRolesListRoute() {
    page.waitForURL(ROLES_LIST_URL);
  }
}
