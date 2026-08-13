/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class CrowdConfigurationPage
    extends BasePage
{
  private static final Locator.GetByRoleOptions SAVE_OPTS =
      new Locator.GetByRoleOptions().setName("Save Configuration").setExact(true);

  private static final Locator.GetByRoleOptions DELETE_OPTS =
      new Locator.GetByRoleOptions().setName("Delete Configuration").setExact(true);

  private static final Locator.GetByRoleOptions TEST_CONNECTION_OPTS =
      new Locator.GetByRoleOptions().setName("Test Connection").setExact(true);

  private static final Locator.GetByRoleOptions OK_OPTS =
      new Locator.GetByRoleOptions().setName("OK").setExact(true);

  private static final Locator.GetByRoleOptions CANCEL_OPTS =
      new Locator.GetByRoleOptions().setName("Cancel").setExact(true);

  public CrowdConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/crowd";
  }

  public Locator container() {
    return locator("#crowd-config-form");
  }

  public Locator serverUrl() {
    return byLabel("Crowd Server URL");
  }

  public Locator applicationName() {
    return byLabel("Application Name");
  }

  public Locator applicationPassword() {
    return byLabel("Application Password");
  }

  public Locator saveButton() {
    return container().getByRole(AriaRole.BUTTON, SAVE_OPTS);
  }

  public Locator deleteButton() {
    return container().getByRole(AriaRole.BUTTON, DELETE_OPTS);
  }

  public Locator testConnectionButton() {
    return container().getByRole(AriaRole.BUTTON, TEST_CONNECTION_OPTS);
  }

  public Locator deleteModal() {
    return locator("#crowd-config-delete-modal");
  }

  public Locator deleteModalSubmitButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, OK_OPTS);
  }

  public Locator deleteModalCancelButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, CANCEL_OPTS);
  }
}
