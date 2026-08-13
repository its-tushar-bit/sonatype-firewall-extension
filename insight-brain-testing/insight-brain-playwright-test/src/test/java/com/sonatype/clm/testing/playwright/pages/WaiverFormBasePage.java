/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Shared base for the two waiver-form pages — Add Waiver ({@link AddWaiverPage}) and Request
 * Waiver ({@link RequestWaiverPage}). Both forms share the same lower-level NxForm building
 * blocks for expiry, reason, and the cancel control (the IDs/classes are page-shared, not
 * page-scoped):
 * <ul>
 * <li>{@code #waiver-expiration-select} — expiry dropdown</li>
 * <li>{@code #waiver-reason-select} — waiver reason dropdown</li>
 * <li>{@code .nx-form__cancel-btn} — NxForm cancel button</li>
 * </ul>
 *
 * <p>
 * Pulling these into a single base eliminates the prior copy/paste in each page object (where
 * the same locators were defined twice and could drift independently — e.g. {@code expiryTimes*}
 * vs {@code expiryTime*} method names) and gives a single seam for the cross-form layout
 * assertion ({@link #assertSharedFormShellLayout(int, int)}).
 *
 * <p>
 * Form-specific bits (component-radio container, scope dropdown id, page title, vulnerability
 * link, etc.) stay on the concrete subclasses because they are scoped to each form's distinct
 * root selector.
 */
public abstract class WaiverFormBasePage
    extends BasePage
{
  protected WaiverFormBasePage() {
    super();
  }

  /**
   * Expiry-time {@code <select>} — {@code id="waiver-expiration-select"}.
   * NxFieldset wraps this in a {@code <fieldset>+<legend>}, not a {@code <label>}, so
   * {@code byLabel()} cannot find it; the stable element id is used instead.
   */
  public Locator expiryTimeSelect() {
    return locator("#waiver-expiration-select");
  }

  /**
   * Expiry-time {@code <option>} list — scoped inside the select element.
   */
  public Locator expiryTimeOptions() {
    return expiryTimeSelect().locator("option");
  }

  /**
   * Waiver-reason {@code <select>} — {@code id="waiver-reason-select"}.
   * NxFieldset wraps this in a {@code <fieldset>+<legend>}, not a {@code <label>}, so
   * {@code byLabel()} cannot find it; the stable element id is used instead.
   */
  public Locator waiverReasonSelect() {
    return locator("#waiver-reason-select");
  }

  /**
   * Waiver-reason {@code <option>} list — scoped inside the select element.
   */
  public Locator waiverReasonOptions() {
    return waiverReasonSelect().locator("option");
  }

  /**
   * NxForm cancel button ("Cancel" accessible name, from NxForm RSC).
   */
  public Locator cancelButton() {
    return byRole(com.microsoft.playwright.options.AriaRole.BUTTON, "Cancel");
  }

  /**
   * Assertions shared by both waiver-form pages: the expiry dropdown is mounted with the
   * expected option count, the reason dropdown is mounted with the expected option count, and
   * the cancel button is visible. The submit button is intentionally not asserted here because
   * its selector differs between the two forms ({@code .add-waiver-submit} vs
   * {@code .request-waiver-submit}); concrete subclasses assert the submit button themselves.
   *
   * @param expectedExpiryOptionCount expected number of {@code <option>} entries in the expiry
   *          dropdown (drives the cross-page consistency check — both forms must surface the
   *          same set of expiry choices to the user)
   * @param expectedWaiverReasonOptionCount expected number of {@code <option>} entries in the
   *          waiver-reason dropdown
   */
  public void assertSharedFormShellLayout(int expectedExpiryOptionCount, int expectedWaiverReasonOptionCount) {
    assertThat(expiryTimeSelect()).isVisible();
    assertThat(expiryTimeOptions()).hasCount(expectedExpiryOptionCount);
    assertThat(waiverReasonSelect()).isVisible();
    assertThat(waiverReasonOptions()).hasCount(expectedWaiverReasonOptionCount);
    assertThat(cancelButton()).isVisible();
  }
}
