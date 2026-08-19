/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class MtiqAdministratorsPage
    extends BasePage
{
  private static final String LIST_ROOT = "#administrators-config-container";

  private static final String EDIT_ROOT = ".iq-administrators-edit";

  public MtiqAdministratorsPage() {
    super();
  }

  public static String listUrl() {
    return "/assets/index.html#/administrators";
  }

  public Locator listContainer() {
    return locator(LIST_ROOT);
  }

  public Locator editContainer() {
    return locator(EDIT_ROOT);
  }

  public Locator listPageHeading() {
    return listContainer().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Administrators").setLevel(1));
  }

  public Locator configureAdministratorsHeading() {
    return listContainer().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Configure Administrators").setLevel(2));
  }

  public Locator rolesTable() {
    return listContainer().getByRole(AriaRole.TABLE);
  }

  public Locator roleRowByName(String roleName) {
    return rolesTable().getByRole(AriaRole.ROW,
        new Locator.GetByRoleOptions().setName(Pattern.compile(escapeForJsRegex(roleName))));
  }

  /** Members cell (second cell) — comma-joined display names. */
  public Locator roleRowMembersText(String roleName) {
    return roleRowByName(roleName).getByRole(AriaRole.CELL).nth(1);
  }

  public Locator editPageHeading() {
    return editContainer().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Configure Administrators").setLevel(1));
  }

  public Locator addMembersHeading() {
    return editContainer().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Add Members").setLevel(2));
  }

  public Locator addMembersForm() {
    return locator("#administrators-add-members-form");
  }

  public Locator searchUsersAndGroupsInput() {
    return addMembersForm().locator(".nx-search-dropdown input.nx-text-input__input");
  }

  public Locator searchMatchOption(String displayText) {
    return addMembersForm().locator(".nx-search-dropdown__menu button.nx-dropdown-button")
        .filter(new Locator.FilterOptions().setHasText(displayText));
  }

  public Locator associatedMembersList() {
    return editContainer().locator(".nx-transfer-list__half");
  }

  public Locator associatedMemberItem(String displayText) {
    return associatedMembersList().getByText(displayText);
  }

  /** Only rendered when the tenant's group-search backend is disabled. */
  public Locator externalGroupInput() {
    return editContainer().locator("#add-associate-group-input");
  }

  public Locator externalGroupAddButton() {
    return editContainer().locator("#add-associate-group-btn");
  }

  public Locator submitButton() {
    // NxStatefulForm may render its footer outside the <form> element; scoping getByRole
    // to the form id causes intermittent timeouts. Use the RSC submit-button class scoped
    // to the edit-page root instead (mirrors on-prem AdministratorsEditPage#submitButton).
    return editContainer().locator(".nx-form__submit-btn");
  }

  /** MTIQ divergence: local username/password inputs must not render on the edit page. */
  public Locator localUsernameInput() {
    return editContainer().getByLabel(Pattern.compile("^Username$", Pattern.CASE_INSENSITIVE));
  }

  public Locator localPasswordInput() {
    return editContainer().getByLabel(Pattern.compile("^Password$", Pattern.CASE_INSENSITIVE));
  }

  public void clickRoleRow(String roleName) {
    roleRowByName(roleName).click();
    page.waitForURL(Pattern.compile(".*#/administrators/.*"));
  }

  public void typeSearch(String query) {
    searchUsersAndGroupsInput().fill(query);
  }
}
