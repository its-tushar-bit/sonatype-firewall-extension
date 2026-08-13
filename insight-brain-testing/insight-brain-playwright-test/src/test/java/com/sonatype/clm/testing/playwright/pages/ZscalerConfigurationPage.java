/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Zscaler Configuration page.
 * Route: {@code /assets/index.html#/firewall/zscalerConfig} — requires the firewall license.
 */
public class ZscalerConfigurationPage
    extends BasePage
{
  private static final String ROOT = "#zscaler-config-page-container";

  public ZscalerConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/firewall/zscalerConfig";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator username() {
    return byLabel("Username");
  }

  public Locator password() {
    return byLabel("Password");
  }

  public Locator hostname() {
    return byLabel("Hostname");
  }

  public Locator apiKey() {
    return byLabel("Zscaler API Key");
  }

  /** Configured Formats fieldset ({@code id="zscaler-config-format"}). */
  public Locator configuredFormatsFieldset() {
    return locator("#zscaler-config-format");
  }

  /** The multi-select toggle inside the Configured Formats dropdown. */
  public Locator configuredFormatsToggle() {
    return configuredFormatsFieldset().getByRole(AriaRole.BUTTON);
  }

  /**
   * A single format option inside the open Configured Formats dropdown. NxCheckbox CSS-hides the
   * underlying {@code <input>} — we return the visible {@code <label>} for interaction/visibility
   * assertions; the input remains addressable via {@link #configuredFormatOptionInput}.
   */
  public Locator configuredFormatOption(String displayName) {
    return configuredFormatsFieldset()
        .locator("label.nx-checkbox")
        .filter(new Locator.FilterOptions().setHasText(displayName));
  }

  /** The (CSS-hidden) input backing a Configured Formats checkbox — for {@code isChecked()} assertions. */
  public Locator configuredFormatOptionInput(String displayName) {
    return configuredFormatsFieldset().getByRole(AriaRole.CHECKBOX,
        new Locator.GetByRoleOptions().setName(displayName));
  }

  /**
   * EULA checkbox. NxCheckbox puts the {@code id} on the outer {@code <label class="nx-checkbox">}
   * wrapper (not the inner {@code <input>}), so this locator resolves directly to the clickable
   * label.
   */
  public Locator eulaCheckbox() {
    return locator("#zscaler-eula-checkbox");
  }

  public Locator eulaLink() {
    return locator("#zscaler-eula-link");
  }

  /**
   * Anchored on {@code .zscaler-submit-button} because the button's visible label is
   * state-dependent — {@code NxStatefulForm} emits "Save" for a fresh form and "Update" after
   * a config exists — so a role+name locator isn't stable.
   */
  public Locator saveButton() {
    return locator(".zscaler-submit-button");
  }

  public Locator cancelButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Cancel").setExact(true));
  }

  public Locator deleteButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Delete Configuration"));
  }

  public Locator testConfigButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Test Configuration"));
  }

  public Locator deleteModal() {
    return locator("#zscaler-config-delete-modal");
  }

  public Locator deleteModalConfirmButton() {
    return deleteModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("OK"));
  }

  public Locator deleteModalCancelButton() {
    return deleteModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Cancel"));
  }

  /** Tooltip icon in the Configured Formats fieldset (performance-benefit hint). */
  public Locator configuredFormatsTooltipIcon() {
    return configuredFormatsFieldset().getByTestId("tooltip-icon");
  }

  /** NxLoadError alert shown when Test Configuration fails (or when isAuthorized=false). */
  public Locator loadError() {
    return nxLoadErrorAlert(container());
  }

  /** NxSuccessAlert shown after a successful Test Configuration call. NxSuccessAlert emits role="status". */
  public Locator testConfigSuccessAlert() {
    return container().getByRole(AriaRole.STATUS);
  }
}
