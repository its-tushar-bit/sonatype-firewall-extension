/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import com.microsoft.playwright.assertions.LocatorAssertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link LegacyViolationsEditorPage}.
 */
public class LegacyViolationsEditorPageAssertions
{
  private final LegacyViolationsEditorPage page;

  public LegacyViolationsEditorPageAssertions(LegacyViolationsEditorPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.heading()).isVisible();
    assertThat(page.heading()).hasText("Legacy Violations");
  }

  /**
   * Asserts all three radios are visible: Inherit from parent, Enabled, and Disabled.
   * Expected on non-root organizations.
   */
  public void shouldShowAllThreeRadios() {
    assertThat(page.inheritFromParentRadio()).isVisible();
    assertThat(page.enabledRadio()).isVisible();
    assertThat(page.disabledRadio()).isVisible();
  }

  /**
   * Asserts only Enabled and Disabled radios are visible; "Inherit from parent" is not rendered.
   * Expected on the root organization ({@code !isRootOrg} gate hides the Inherit radio).
   */
  public void shouldShowOnlyEnabledAndDisabledRadios() {
    assertThat(page.inheritFromParentRadio()).isHidden();
    assertThat(page.enabledRadio()).isVisible();
    assertThat(page.disabledRadio()).isVisible();
  }

  /**
   * Asserts the "Allow configuration to be overridden…" checkbox is visible.
   * Expected on organizations (both root and non-root).
   */
  public void shouldShowAllowOverrideCheckbox() {
    assertThat(page.allowOverrideCheckbox()).isVisible();
  }

  /**
   * Asserts the "Allow configuration to be overridden…" checkbox is not rendered.
   * Expected on applications ({@code !isApp} gate in {@code LegacyViolationsEditor.jsx}).
   */
  public void shouldNotShowAllowOverrideCheckbox() {
    assertThat(page.allowOverrideCheckbox()).isHidden();
  }

  /**
   * Asserts the "no changes to save" validation error alert is visible.
   * RSC's {@code NxStatefulForm} does NOT disable the submit button when
   * {@code validationErrors = MSG_NO_CHANGES_TO_SAVE}; clicking Update without
   * changes sets {@code showValidationErrors = true}, making this alert visible.
   */
  public void shouldShowNoChangesValidationError() {
    assertThat(page.noChangesValidationError()).isVisible();
  }

  /**
   * Asserts the license-gate error alert is visible with the expected message.
   * Expected when {@code areLegacyViolationsSupported} is {@code false}.
   * Uses {@link PlaywrightTiming#SLOW_ELEMENT_TIMEOUT_MS} because the alert is rendered inside
   * {@code NxLoadWrapper}, which must finish loading the legacyViolations config before the
   * inner content (and therefore the alert) becomes visible.
   */
  public void shouldShowLicenseErrorAlert() {
    assertThat(page.licenseErrorAlert()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
    assertThat(page.licenseErrorAlert()).containsText("not supported by your license");
  }

  /**
   * Asserts the "parent cannot override" info alert is NOT present.
   * Expected when the parent org has "Allow configuration to be overridden" enabled,
   * meaning the child org is allowed to change its Legacy Violations setting.
   */
  public void shouldNotShowParentOverrideDisabledAlert() {
    assertThat(page.parentOverrideDisabledAlert()).isHidden();
  }
}
