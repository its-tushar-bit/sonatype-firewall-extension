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

import static com.sonatype.clm.testing.playwright.pages.BasePage.escapeForJsRegex;
import static com.sonatype.clm.testing.playwright.pages.CommonButtonOptions.CONTINUE_BUTTON_OPTS;
import static com.sonatype.clm.testing.playwright.pages.CommonButtonOptions.SAVE_BUTTON_OPTS;

public class MtiqUserManagementPage
    extends BasePage
{
  private static final String ROOT = "#user-management";

  public MtiqUserManagementPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/users";
  }

  /** Unlabelled layout section — id used only as a scope for role-based lookups. */
  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageTitle() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Configure Users").setLevel(2));
  }

  public Locator inviteFormTitle() {
    return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Invite User").setLevel(1));
  }

  public Locator inviteUserButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Invite User"));
  }

  public Locator loadError() {
    return nxLoadErrorAlert(container());
  }

  public Locator inviteFirstNameInput() {
    return byLabel("First Name");
  }

  public Locator inviteLastNameInput() {
    return byLabel("Last Name");
  }

  public Locator inviteEmailInput() {
    return byLabel("Email");
  }

  /** NxStatefulForm has {@code role="form"} but no accessible name — anchored by id. */
  public Locator inviteForm() {
    return locator("#user-form");
  }

  public Locator validationErrorFor(Locator input) {
    return nxFieldValidationMessage(input);
  }

  public Locator userList() {
    return container().getByRole(AriaRole.LIST);
  }

  /**
   * Word-boundary containment pattern so UUID-prefix rows can't both match; JS-regex-safe (no
   * {@code \Q\E}). Word-boundary, not whole-string equality — the row's rendered text also
   * contains the display name, so the match must be contained-anywhere but bounded by
   * non-word/dash chars.
   */
  public Locator userListItemFor(String usernameOrDisplayText) {
    Pattern wordBounded = Pattern.compile("(?<![\\w-])" + escapeForJsRegex(usernameOrDisplayText) + "(?![\\w-])");
    return userList()
        .getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText(wordBounded));
  }

  public Locator deleteButtonFor(String usernameOrDisplayText) {
    return userListItemFor(usernameOrDisplayText)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete user"));
  }

  /** Id-anchored: NxModal portals to body and has no aria-labelledby, so DIALOG-by-name misses. */
  public Locator deleteUserModal() {
    return locator("#delete-user-modal");
  }

  public Locator deleteUserModalSubmit() {
    return deleteUserModal().getByRole(AriaRole.BUTTON, CONTINUE_BUTTON_OPTS);
  }

  public Locator inviteSubmitButton() {
    return inviteForm().getByRole(AriaRole.BUTTON, SAVE_BUTTON_OPTS);
  }
}
