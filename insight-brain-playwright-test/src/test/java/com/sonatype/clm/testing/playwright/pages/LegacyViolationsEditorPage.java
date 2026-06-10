/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Playwright page object for the Legacy Violations editor.
 * <p>
 * URL suffix: {@code /legacyViolations} under both org and app edit shells:<br>
 * Org — {@code …/management/edit/organization/{orgId}/legacyViolations}<br>
 * App — {@code …/management/edit/application/{appPublicId}/legacyViolations}
 * <p>
 * The form renders conditionally based on context:
 * <ul>
 * <li><b>Non-root org:</b> 3 radios (Inherit, Enabled, Disabled) + Allow Override checkbox.</li>
 * <li><b>Root org:</b> "Inherit from parent" radio omitted ({@code !isRootOrg} gate).</li>
 * <li><b>Application:</b> "Allow configuration to be overridden" checkbox omitted ({@code !isApp} gate).</li>
 * </ul>
 */
public class LegacyViolationsEditorPage
    extends BasePage
{
  /** URL path suffix shared by org and app Legacy Violations edit routes. */
  public static final String LEGACY_VIOLATIONS_URL_FRAGMENT = "/legacyViolations";

  public LegacyViolationsEditorPage() {
    super();
  }

  /** Level-1 page heading: "Legacy Violations". */
  public Locator heading() {
    return locator("h1");
  }

  /**
   * Visible label element for the "Inherit from parent" radio.
   * RSC {@code NxRadio} renders an {@code .nx-radio-checkbox} label; we filter by visible text.
   * Only rendered for non-root organizations.
   */
  public Locator inheritFromParentRadio() {
    return locator(".nx-radio-checkbox").filter(
        new Locator.FilterOptions().setHasText("Inherit from parent"));
  }

  /**
   * Visible label element for the "Enabled" radio.
   * Rendered for all contexts (org and app).
   */
  public Locator enabledRadio() {
    return locator(".nx-radio-checkbox").filter(
        new Locator.FilterOptions().setHasText("Enabled")).first();
  }

  /**
   * Visible label element for the "Disabled" radio.
   * Rendered for all contexts (org and app).
   */
  public Locator disabledRadio() {
    return locator(".nx-radio-checkbox").filter(
        new Locator.FilterOptions().setHasText("Disabled")).first();
  }

  /**
   * Visible label for the "Allow configuration to be overridden…" checkbox.
   * Only rendered for organizations ({@code !isApp} gate in {@code LegacyViolationsEditor.jsx}).
   */
  public Locator allowOverrideCheckbox() {
    return locator(".nx-radio-checkbox").filter(
        new Locator.FilterOptions().setHasText("Allow configuration to be overridden"));
  }

  /** Submit ("Update") button of the Legacy Violations editor form, scoped to the tile. */
  public Locator submitButton() {
    return locator(".nx-tile .nx-form__submit-btn");
  }

  /**
   * {@code NxErrorAlert} rendered in the form footer by RSC's {@code NxForm} when
   * {@code formHasValidationErrors} is true. Becomes visible (via CSS) after the user
   * clicks the submit button while {@code validationErrors = MSG_NO_CHANGES_TO_SAVE}
   * ({@code isDirty = false}). RSC's {@code NxStatefulForm} does NOT disable the button;
   * instead, clicking it with errors sets {@code showValidationErrors = true} which
   * adds the {@code nx-form--show-validation-errors} class and reveals this alert.
   */
  public Locator noChangesValidationError() {
    return locator(".nx-form__validation-errors");
  }

  /**
   * {@code NxErrorAlert} rendered when {@code areLegacyViolationsSupported} is {@code false}.
   * Text: "Legacy Violations are not supported by your license."
   */
  public Locator licenseErrorAlert() {
    return locator(".nx-alert--error");
  }

  /**
   * {@code NxInfoAlert} rendered when the parent org has disabled override (id from JSX).
   * Text: "The parent selection cannot be overridden."
   */
  public Locator parentOverrideDisabledAlert() {
    return locator("#legacy-violations-disabled-message");
  }

  /** Clicks the "Enabled" radio label to select that Legacy Violation status. */
  public void clickEnabledRadio() {
    enabledRadio().click();
  }

  /**
   * Ensures the "Allow configuration to be overridden…" checkbox is checked.
   * Checks the current input state; clicks the label only if the checkbox is unchecked.
   */
  public void ensureAllowOverrideChecked() {
    Locator input = allowOverrideCheckbox().locator("input");
    if (!input.isChecked()) {
      allowOverrideCheckbox().click();
    }
  }

  /**
   * Ensures the "Allow configuration to be overridden…" checkbox is unchecked.
   * Clicks the label only if the checkbox is currently checked.
   */
  public void uncheckAllowOverride() {
    Locator input = allowOverrideCheckbox().locator("input");
    if (input.isChecked()) {
      allowOverrideCheckbox().click();
    }
  }

  /** Clicks the form's Update/Create submit button. */
  public void submit() {
    submitButton().click();
  }

  /**
   * Waits for the {@code NxSubmitMask} success overlay to appear then auto-dismiss.
   * {@code NxStatefulForm} shows the checkmark overlay for ~800 ms then resets state.
   */
  public void waitForSaveSuccess() {
    Locator successMask = page.locator(".nx-submit-mask--success");
    successMask.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.VISIBLE)
        .setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
    successMask.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN)
        .setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  /**
   * Returns {@code true} when the Legacy Violations form is rendered (feature licensed).
   * Waits up to {@link PlaywrightTiming#SLOW_ELEMENT_TIMEOUT_MS} for the radios to appear,
   * since the content is inside {@code NxLoadWrapper} and loads asynchronously.
   * Returns {@code false} if the feature is unavailable (error alert shown instead).
   */
  public boolean isFormRendered() {
    try {
      locator(".nx-radio-checkbox").first()
          .waitFor(
              new Locator.WaitForOptions()
                  .setState(WaitForSelectorState.VISIBLE)
                  .setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
      return true;
    }
    catch (TimeoutError e) {
      return false;
    }
  }
}
