/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Add Waiver page.
 *
 * <p>
 * Extends {@link WaiverFormBasePage} for the cross-form NxForm building blocks (expiry select,
 * reason select, cancel button) so the same locators don't drift between this class and
 * {@link RequestWaiverPage}.
 */
public class AddWaiverPage
    extends WaiverFormBasePage
{
  private static final String ROOT = "#add-waiver-page";

  public AddWaiverPage() {
    super();
  }

  public static String url(String violationId) {
    return "/assets/index.html#/addWaiver/" + violationId;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator artifactName() {
    return locator(ROOT + " .iq-add-waiver-form__component .nx-read-only__label span");
  }

  public Locator componentName() {
    return locator(ROOT + " .iq-add-waiver-form__component .nx-read-only__data");
  }

  public Locator policyName() {
    return locator(ROOT + " .iq-add-waiver-form__policy .iq-threat-level");
  }

  public Locator constraintName() {
    return locator(ROOT + " .iq-add-waiver-form__constraint .nx-read-only__data");
  }

  public Locator currentUserName() {
    return locator(ROOT + " .iq-add-waiver-form__created-by .nx-read-only__data");
  }

  public Locator conditions() {
    return locator(ROOT + " .iq-add-waiver-form__conditions .nx-read-only__data span");
  }

  public Locator vulnerabilityDetailsLink() {
    return locator(ROOT + " .iq-add-waiver-form__vulnerability_details_link a");
  }

  public Locator scopesDropdown() {
    return locator("#iq-add-waiver-scope");
  }

  public Locator scopeOptions() {
    return locator("#iq-add-waiver-scope option");
  }

  public Locator componentRadios() {
    return locator(ROOT + " .iq-add-waiver-form__components .nx-radio");
  }

  public Locator componentRadioInput(int index) {
    return componentRadios().nth(index).locator(".nx-radio__input");
  }

  public Locator componentRadioLabel(int index) {
    return componentRadios().nth(index).locator(".nx-radio__content");
  }

  // Shared NxForm building blocks (expiryTimeSelect, expiryTimeOptions, waiverReasonSelect,
  // waiverReasonOptions, cancelButton) are inherited from WaiverFormBasePage so they stay in
  // sync with RequestWaiverPage.

  public Locator comments() {
    // NxFieldset renders <fieldset>+<legend>, not <label>, so byLabel() cannot match.
    return locator(ROOT + " .iq-add-waiver-form__comments .nx-text-input__input");
  }

  public Locator saveButton() {
    // The AddWaiver form's submit button uses a custom CSS class — stable and unique.
    return locator(".add-waiver-submit");
  }

  public Locator submitError() {
    return locator(ROOT + " .nx-footer .nx-alert");
  }

  public Locator customExpiryTime() {
    return locator(ROOT + " .iq-add-waiver-form__date-input .nx-text-input__input");
  }

  public Locator expiryTimeMessage() {
    return locator(ROOT + " .iq-add-waiver-form__expiration-days-diff");
  }

  // --------------- Actions ---------------

  public void selectScope(String scopeLabel) {
    assertThat(container())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS));
    assertThat(scopesDropdown())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS));
    scopesDropdown().selectOption(
        scopeLabel,
        new Locator.SelectOptionOptions().setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS));
  }

  public void selectComponentRadio(int index) {
    Locator label = componentRadioLabel(index);
    label.scrollIntoViewIfNeeded();
    label.click();
  }

  public void fillComment(String text) {
    comments().fill(text);
  }

  public void submit() {
    saveButton().click();
  }

}
