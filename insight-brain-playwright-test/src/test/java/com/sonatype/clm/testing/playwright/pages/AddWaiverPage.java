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

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AddWaiverPage
    extends WaiverFormBasePage
{
  private static final String ROOT = "#add-waiver-page";

  private static final Pattern ALL_VERSIONS_PATTERN = Pattern.compile("all versions", Pattern.CASE_INSENSITIVE);

  private static final Locator.FilterOptions ALL_VERSIONS_FILTER =
      new Locator.FilterOptions().setHasText(ALL_VERSIONS_PATTERN);

  private static final Locator.GetByRoleOptions SCOPE_GROUP_OPTS =
      new Locator.GetByRoleOptions().setName("Scope");

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

  public Locator comments() {
    return locator(ROOT + " .iq-add-waiver-form__comments .nx-text-input__input");
  }

  public Locator saveButton() {
    return locator(".add-waiver-submit");
  }

  public Locator submitError() {
    return locator(ROOT + " .nx-footer .nx-alert");
  }

  public Locator scopeFieldset() {
    return container().getByRole(AriaRole.GROUP, SCOPE_GROUP_OPTS);
  }

  public Locator scopeFieldsetLabel() {
    return scopeFieldset().getByText("Scope");
  }

  public Locator allVersionsRadio() {
    return container().locator(".nx-radio").filter(ALL_VERSIONS_FILTER);
  }

  public Locator allVersionsRadioInput() {
    return container().getByLabel(ALL_VERSIONS_PATTERN);
  }

  public Locator customExpiryTime() {
    return locator(ROOT + " .iq-add-waiver-form__date-input .nx-text-input__input");
  }

  public Locator expiryTimeMessage() {
    return locator(ROOT + " .iq-add-waiver-form__expiration-days-diff");
  }

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

  public void selectExpiryTime(String label) {
    expiryTimeSelect().selectOption(label);
  }

  public void fillCustomExpiryDate(String date) {
    customExpiryTime().fill(date);
  }

  public void clickCancel() {
    cancelButton().click();
  }

  public void submit() {
    saveButton().click();
  }

}
