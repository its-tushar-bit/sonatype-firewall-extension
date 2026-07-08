/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Source Control Configuration editor
 * ({@code SourceControlConfiguration.jsx} / {@code RootSourceControlConfiguration.jsx}).
 *
 * <p>
 * The root container is {@code #source-control-editor}; the form lives inside
 * {@code .iq-source-control-configuration-tile}. Provider options come from
 * {@code ScmProviderOptions.jsx} and toggle ids from {@code utils.js#SOURCE_CONTROL_OPTIONS}.
 */
public class SourceControlConfigurationPage
    extends BasePage
{
  /** Hash route segment used in {@code OrgsAndPolicies/route.js} for the SCM editor. */
  public static final String URL_FRAGMENT = "/source-control";

  public SourceControlConfigurationPage() {
    super();
  }

  public Locator container() {
    return locator("#source-control-editor");
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Source Control Configuration"));
  }

  public Locator providerLabel() {
    return locator("#source-control-provider .nx-label__text");
  }

  public Locator providerSelect() {
    return locator("#source-control-provider-select");
  }

  public Locator accessTokenLabel() {
    return container().getByText("Access Token", new Locator.GetByTextOptions().setExact(true));
  }

  public Locator accessTokenInput() {
    return locator("#source-control-token");
  }

  public Locator defaultBranchLabel() {
    return locator("#source-control-default-branch .nx-label__text");
  }

  public Locator defaultBranchInput() {
    return locator("#editor-source-control-branch");
  }

  /** Toggle root for {@code label#<id>} pattern from {@code SOURCE_CONTROL_OPTIONS}. */
  public Locator toggle(String toggleId) {
    return locator("label#" + toggleId);
  }

  public Locator toggleTitle(String toggleId) {
    return toggle(toggleId).locator(".iq-source-control-toggle__title");
  }

  public Locator advancedGitOptionsBlock() {
    return locator(".git-advanced-options");
  }

  public Locator submitButton() {
    return container().locator("button.nx-form__submit-btn");
  }

  public void selectProvider(String providerLabel) {
    providerSelect().selectOption(providerLabel);
  }

  /**
   * Error alert rendered when neither the {@code notifications} nor {@code automation}
   * product feature is licensed, forcing {@code selectIsSourceControlForSourceTileSupported}
   * to {@code false}.
   * {@code NxErrorAlert} renders with {@code role="alert"}.
   */
  public Locator unsupportedAlert() {
    return container().getByRole(AriaRole.ALERT);
  }

  /**
   * Prepare GitHub + PAT auth so {@link #accessTokenInput()} is mounted.
   *
   * <p>
   * When the GitHub App feature flag is enabled the SCM editor renders
   * {@code #github-authentication-method} with inheritance radios (Inherit / Override) and
   * auth-type radios (GitHub App / Personal Access Token). The Access Token field only mounts
   * after selecting Override (at org/app level) <em>and</em> Personal Access Token. When the
   * feature flag is off the token field mounts directly after selecting a provider — no extra
   * steps needed.
   *
   * <p>
   * All waits use {@link PlaywrightTiming} constants so they stay aligned with the rest of the
   * module.
   */
  public void selectGitHubPersonalAccessTokenCredentials() {
    selectProvider("GitHub");

    Locator authFieldset = locator("#github-authentication-method");
    if (authFieldset.count() == 0) {
      return;
    }
    assertThat(authFieldset).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    // NxRadio CSS-hides its underlying <input> element; use the visible label text for
    // both visibility checks and clicks to avoid false "hidden" failures.
    Locator overrideLabel = authFieldset.getByText("Override",
        new Locator.GetByTextOptions().setExact(true));
    if (overrideLabel.count() > 0 && overrideLabel.first().isVisible()) {
      overrideLabel.first().click();
    }

    Locator patLabel = authFieldset.getByText("Personal Access Token",
        new Locator.GetByTextOptions().setExact(true));
    assertThat(patLabel).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    patLabel.click();
  }

}
