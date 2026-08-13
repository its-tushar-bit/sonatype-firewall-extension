/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

/**
 * Playwright page object for the "Revoke Legacy Violation Status" confirmation modal.
 * <p>
 * Opened from the application owner summary Actions dropdown via
 * {@code handleRevokeLegacyViolationStatus()} in {@code ActionDropdown.jsx}.
 * Modal id: {@code #revoke-legacy-violation-modal}; submit button text: "Revoke".
 * On success, Redux navigates to the app's org summary via {@code stateGo}.
 */
public class RevokeLegacyViolationModalPage
    extends BasePage
{
  public RevokeLegacyViolationModalPage() {
    super();
  }

  /** Root {@code NxModal} element ({@code id="revoke-legacy-violation-modal"}). */
  public Locator modal() {
    return locator("#revoke-legacy-violation-modal");
  }

  /** Modal heading ({@code NxH2}: "Revoke Legacy Violation Status"). */
  public Locator heading() {
    return modal().locator("h2");
  }

  /** Submit button inside the modal form (submit text: "Revoke"). */
  public Locator submitButton() {
    return modal().locator(".nx-form__submit-btn");
  }

  /** Clicks the "Revoke" submit button to confirm revoking legacy status. */
  public void submit() {
    submitButton().click();
  }

  /**
   * Waits for the modal to close.
   * After the PUT succeeds, {@code startSaveMaskSuccessTimer} fires {@code closeModal},
   * unmounting the modal from the DOM.
   */
  public void waitForModalToClose() {
    modal().waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN)
        .setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }
}
