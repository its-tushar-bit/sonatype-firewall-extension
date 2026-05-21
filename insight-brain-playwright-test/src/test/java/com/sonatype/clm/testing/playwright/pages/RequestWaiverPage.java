/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Request Waiver page.
 *
 * <p>
 * Extends {@link WaiverFormBasePage} for the cross-form NxForm building blocks (expiry select,
 * reason select, cancel button) so the same locators don't drift between this class and
 * {@link AddWaiverPage}.
 */
public class RequestWaiverPage
    extends WaiverFormBasePage
{
  private static final String ROOT = "#request-waiver-page";

  public RequestWaiverPage() {
    super();
  }

  public static String url(String violationId) {
    return "/assets/index.html#/requestWaiver/" + violationId;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator title() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Request Waiver").setLevel(1));
  }

  public Locator subtitle() {
    return container().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
  }

  public Locator componentSection() {
    return locator(ROOT + " .iq-request-waiver-form__component");
  }

  public Locator policySection() {
    return locator(ROOT + " .iq-request-waiver-form__policy");
  }

  public Locator constraintSection() {
    return locator(ROOT + " .iq-request-waiver-form__constraint");
  }

  public Locator conditionsSection() {
    return locator(ROOT + " .iq-request-waiver-form__conditions");
  }

  public Locator scopeSelect() {
    return locator("#iq-request-waiver-scope");
  }

  public Locator scopeOptions() {
    return locator("#iq-request-waiver-scope option");
  }

  public Locator componentRadios() {
    return locator(ROOT + " .iq-request-waiver-form__components .nx-radio__input");
  }

  public Locator componentRadioLabels() {
    return locator(ROOT + " .iq-request-waiver-form__components .nx-radio__content");
  }

  // Shared NxForm building blocks (expiryTimeSelect, expiryTimeOptions, waiverReasonSelect,
  // waiverReasonOptions, cancelButton) are inherited from WaiverFormBasePage so they stay in
  // sync with AddWaiverPage.

  public Locator comments() {
    // NxFieldset renders <fieldset>+<legend>, not <label>, so byLabel() cannot match.
    return locator(ROOT + " .iq-request-waiver-form__comments .nx-text-input__input");
  }

  public Locator noteToReviewer() {
    // NxFieldset renders <fieldset>+<legend>, not <label>, so byLabel() cannot match.
    return locator(ROOT + " .iq-request-waiver-form__note-to-reviewer .nx-text-input__input");
  }

  public Locator customExpiryDateInput() {
    return locator(ROOT + " .iq-request-waiver-form__date-input .nx-text-input__input");
  }

  public Locator submitButton() {
    return byRole(AriaRole.BUTTON, "Submit");
  }

  public Locator submitError() {
    return locator(ROOT + " .nx-footer .nx-alert");
  }

  public Locator errorAlert() {
    return locator(ROOT + " .nx-alert--error");
  }

  // --------------- Actions ---------------

  public void selectWaiverReason(String reason) {
    waiverReasonSelect().selectOption(reason);
  }

  public void fillComment(String comment) {
    comments().fill(comment);
  }

  public void fillNoteToReviewer(String note) {
    noteToReviewer().fill(note);
  }

  public void submit() {
    submitButton().click();
  }

}
