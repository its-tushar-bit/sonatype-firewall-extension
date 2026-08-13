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
 * Page object for the Request Waiver Update flow.
 * Covers the Waiver Requests dashboard tab (entry point) and the update form.
 */
public class RequestWaiverUpdatePage
    extends BasePage
{
  private static final String FORM_ROOT = "#request-waiver-page";

  public RequestWaiverUpdatePage() {
    super();
  }

  // --------------- URL ---------------

  public static String url() {
    return "/assets/index.html#/dashboard/waiverRequests";
  }

  // --------------- Dashboard tile ---------------

  public Locator firstWaiverRequestTile() {
    return locator(".iq-dashboard-waiver-request").first();
  }

  public void waitUntilSpinnersGone() {
    assertThat(locator(".nx-loading-spinner").first())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  // --------------- Update form ---------------

  public Locator container() {
    return locator(FORM_ROOT);
  }

  public Locator title() {
    return container().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  public Locator componentSection() {
    return locator(FORM_ROOT + " .iq-request-waiver-form__component");
  }

  public Locator policySection() {
    return locator(FORM_ROOT + " .iq-request-waiver-form__policy");
  }

  public Locator comments() {
    // NxFieldset renders <fieldset>+<legend>, not <label>, so byLabel() cannot match.
    return locator(FORM_ROOT + " .iq-request-waiver-form__comments .nx-text-input__input");
  }

  public Locator noteToReviewer() {
    // NxFieldset renders <fieldset>+<legend>, not <label>, so byLabel() cannot match.
    return locator(FORM_ROOT + " .iq-request-waiver-form__note-to-reviewer .nx-text-input__input");
  }

  public Locator submitButton() {
    return byRole(AriaRole.BUTTON, "Submit");
  }

  public Locator errorAlert() {
    return locator(FORM_ROOT + " .nx-alert--error");
  }

  // --------------- Actions ---------------

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
