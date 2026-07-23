/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

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
}
