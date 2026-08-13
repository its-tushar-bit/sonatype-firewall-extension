/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class MailConfigurationPage
    extends BasePage
{
  private static final String ROOT = "#mail-config-page-container";

  private static final String DELETE_MODAL = "#mail-config-delete-modal";

  public MailConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/mailConfig";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator tileHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Email").setExact(true));
  }

  public Locator hostnameInput() {
    return container().getByLabel("Hostname");
  }

  public Locator portInput() {
    return container().getByLabel("Port");
  }

  public Locator usernameInput() {
    return container().getByLabel("Username");
  }

  public Locator passwordInput() {
    return container().getByLabel("Password");
  }

  public Locator systemEmailInput() {
    return container().getByLabel("System Email");
  }

  public Locator testEmailRecipientInput() {
    return container().getByLabel("Test Configuration");
  }

  public Locator sendTestEmailButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Send Test Email").setExact(true));
  }

  public Locator saveButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.SAVE_BUTTON_OPTS);
  }

  public Locator cancelButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator deleteButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Delete Configuration").setExact(true));
  }

  public Locator deleteModal() {
    return locator(DELETE_MODAL);
  }

  public Locator deleteModalSubmitButton() {
    return locator(DELETE_MODAL + " .nx-form__submit-btn");
  }

  public Locator deleteModalCancelButton() {
    return locator(DELETE_MODAL + " .nx-form__cancel-btn");
  }

  public void openDeleteModal() {
    deleteButton().click();
    assertThat(deleteModal()).isVisible();
  }

  public Locator testEmailErrorAlert() {
    return container().getByRole(AriaRole.ALERT)
        .filter(new Locator.FilterOptions().setHasText("test email"));
  }
}
