/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Base URL Configuration page (Lifecycle).
 * {@code insight-brain-frontend/src/main/frontend/configuration/baseUrl/BaseUrlConfiguration.jsx}.
 */
public class BaseUrlConfigurationPage
    extends BasePage
{
  /** Tile that wraps the form — used as a strict-mode anchor for the form's buttons. */
  private static final String FORM_TILE = "#base-url-config-form";

  /** Confirmation modal shown after clicking {@link #deleteButton()}. */
  private static final String DELETE_MODAL = "#base-url-config-delete-modal";

  public BaseUrlConfigurationPage() {
    super();
  }

  /**
   * Hash route for the standalone (Lifecycle) Base URL Configuration page. The frontend
   * registers this state at {@code configuration/baseUrl/route.js} as
   * {@code name: "baseUrlConfiguration", url: "/baseUrl"} → so the address is
   * {@code #/baseUrl}, NOT {@code #/baseUrlConfiguration} (the latter is an SPA state name,
   * not the URL).
   */
  public static String url() {
    return "/assets/index.html#/baseUrl";
  }

  public static String notSetNoticeUrl() {
    return "/assets/index.html#/baseUrlNotSetNotice";
  }

  public Locator baseUrlAttribute() {
    return byLabel("Base URL");
  }

  public Locator notSetNoticeBanner() {
    return locator("#base-url-not-set-notice");
  }

  /**
   * Save Configuration button. Anchored under the form tile because the
   * {@code .iq-base-url-configuration-save-button} class is also present on the submit
   * button inside the {@link #DELETE_MODAL}, which would otherwise trigger a strict-mode
   * violation when the modal is open.
   */
  public Locator saveButton() {
    return locator(FORM_TILE + " .iq-base-url-configuration-save-button");
  }

  public Locator cancelButton() {
    return locator("#base-url-cancel");
  }

  public Locator deleteButton() {
    return locator("#base-url-config-delete-button");
  }

  // --------------- Delete confirmation modal ---------------

  public Locator deleteModal() {
    // NxModal does not set aria-labelledby in this RSC version — use the stable id.
    return locator(DELETE_MODAL);
  }

  /**
   * "OK" submit button on the delete-confirmation modal. Anchored under
   * {@link #DELETE_MODAL} so it never collides strict-mode with {@link #saveButton()} (both
   * are {@code NxStatefulForm} submit buttons and share the {@code .nx-form__submit-btn} class).
   */
  public Locator deleteModalSubmitButton() {
    return locator(DELETE_MODAL + " .nx-form__submit-btn");
  }

  /** Cancel button on the delete-confirmation modal — anchored as per {@link #deleteModalSubmitButton()}. */
  public Locator deleteModalCancelButton() {
    return locator(DELETE_MODAL + " .nx-form__cancel-btn");
  }

  /** Click the page's delete button and wait for the confirmation modal to render. */
  public void openDeleteModal() {
    deleteButton().click();
    assertThat(deleteModal()).isVisible();
  }

  /** Click the modal's Cancel button and wait for the modal to detach. */
  public void cancelDeleteModal() {
    deleteModalCancelButton().click();
    assertThat(deleteModal()).isHidden();
  }
}
