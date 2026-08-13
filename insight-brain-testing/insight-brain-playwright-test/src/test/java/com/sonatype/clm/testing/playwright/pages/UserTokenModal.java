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
 * Page object for the Manage User Token modal ({@code UserTokenModal.jsx}).
 *
 * <p>
 * The modal has three distinct UI states driven by the {@code userToken} prop:
 * <ul>
 * <li><b>Initial</b> — {@code userToken === false}: Generate button shown, no codes, no alert.</li>
 * <li><b>Generated</b> — {@code userToken} is the credentials object: user/pass code inputs shown,
 * Generate and Delete buttons hidden.</li>
 * <li><b>Existing</b> — {@code userToken === true}: existence alert + Delete button shown,
 * optionally with the expiration block when an expiration policy is configured.</li>
 * </ul>
 * Tests should prefer the {@link #shouldShowInitialState()}, {@link #shouldShowGeneratedCredentials()},
 * and {@link #shouldShowExistingTokenState()} helpers over poking at individual locators.
 */
public class UserTokenModal
    extends BasePage
{
  private static final String ROOT = "#user-token-modal";

  public UserTokenModal() {
    super();
  }

  public Locator modal() {
    // NxModal does not set aria-labelledby in this RSC version — use the stable id.
    return locator(ROOT);
  }

  public Locator generateUserTokenButton() {
    return modal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Generate User Token"));
  }

  public Locator deleteUserTokenButton() {
    return modal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete User Token"));
  }

  public Locator userCodeInput() {
    // NxFormGroup label="User Code" → NxTextInput id="user-token-usercode"
    return locator("#user-token-usercode");
  }

  public Locator passCodeInput() {
    // JSX label is "Passcode" (one word, no space) with id="user-token-passcode".
    // getByLabel("Pass Code") does not match — use the stable id instead.
    return locator("#user-token-passcode");
  }

  public Locator tokenExistenceAlert() {
    // The modal has two role="alert" elements (NxWarningAlert + NxErrorAlert), so getByRole(ALERT)
    // causes a strict-mode violation when both are in the DOM. Use the stable id instead.
    return locator(ROOT + " #user-token-modal-token-exists-alert");
  }

  public Locator cancelButton() {
    return modal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close"));
  }

  public Locator expirationSection() {
    return locator(ROOT + " .iq-user-token-expiration");
  }

  public Locator expirationHeading() {
    return locator(ROOT + " .iq-user-token-expiration__heading");
  }

  public Locator expirationSubtitle() {
    return locator(ROOT + " .iq-user-token-expiration__subtitle");
  }

  public Locator expirationDate() {
    return locator(ROOT + " .iq-user-token-expiration__date");
  }

  // --------------- Business actions ---------------

  public void generateToken() {
    assertThat(generateUserTokenButton())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
    generateUserTokenButton().click();
  }

  public void deleteToken() {
    deleteUserTokenButton().click();
  }

  public void close() {
    cancelButton().click();
    assertThat(modal())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

}
