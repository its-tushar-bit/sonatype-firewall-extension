/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class WebhookEditorPage
    extends BasePage
{
  private static final String ROOT = "#webhook-editor";

  private static final Locator.GetByRoleOptions CREATE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Create").setExact(true);

  private static final Locator.GetByRoleOptions UPDATE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Update").setExact(true);

  private static final Locator.GetByRoleOptions DELETE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Delete Webhook");

  private static final Locator.GetByRoleOptions CONTINUE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Continue");

  private static final Locator.GetByRoleOptions H1_OPTS =
      new Locator.GetByRoleOptions().setLevel(1);

  public WebhookEditorPage() {
    super();
  }

  public static String createUrl() {
    return "/assets/index.html#/webhooks/create";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageTitle() {
    return locator(ROOT).getByRole(AriaRole.HEADING, H1_OPTS);
  }

  public Locator urlInput() {
    return locator("#editor-webhook-url");
  }

  public Locator descriptionInput() {
    return locator("#editor-webhook-description");
  }

  public Locator secretKeyInput() {
    return locator("#editor-webhook-secret-key");
  }

  public Locator eventTypesFieldset() {
    return locator("#event-types");
  }

  public Locator eventTypeCheckbox(String eventTypeName) {
    return locator("#event-types").getByText(eventTypeName);
  }

  public Locator eventTypeCheckboxInput(String eventTypeName) {
    return eventTypesFieldset().getByLabel(eventTypeName);
  }

  public Locator eventTypeLabels() {
    return eventTypesFieldset().locator(".nx-checkbox__content");
  }

  public Locator submitButton() {
    return locator("#webhook-form").getByRole(AriaRole.BUTTON, CREATE_BUTTON_OPTS)
        .or(locator("#webhook-form").getByRole(AriaRole.BUTTON, UPDATE_BUTTON_OPTS));
  }

  public Locator cancelButton() {
    return locator("#webhook-form").getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator validationError() {
    return locator(ROOT + " [role='alert'][aria-label='form validation errors']");
  }

  public Locator httpInfoAlert() {
    return locator("#editor-webhook-url-http-alert");
  }

  public Locator deleteButton() {
    return locator(ROOT).getByRole(AriaRole.BUTTON, DELETE_BUTTON_OPTS);
  }

  public Locator deleteModal() {
    return locator("#delete-modal");
  }

  public Locator deleteModalContinueButton() {
    return locator("#delete-modal").getByRole(AriaRole.BUTTON, CONTINUE_BUTTON_OPTS);
  }

  public Locator deleteModalWarningText() {
    return locator("#delete-modal .nx-alert--warning");
  }

  public Locator httpWarningModal() {
    return locator("#http-url-warning-modal");
  }

  public Locator httpWarningModalContinueButton() {
    return locator("#http-url-warning-modal").getByRole(AriaRole.BUTTON, CONTINUE_BUTTON_OPTS);
  }

  public Locator loadError() {
    return locator(ROOT + " .nx-alert--load-error");
  }

}
