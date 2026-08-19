/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import static com.sonatype.clm.testing.playwright.utils.LocatorRoleOptions.withLevel;

/**
 * Regression-only locators and actions for the Proprietary Component Configuration editor.
 * Covers selectors needed by rows 99–102 of the manual regression suite that are not
 * present on any existing page object.
 *
 * <p>
 * Do NOT add methods to any existing page object.
 */
public class ProprietaryComponentsRegressionPage
    extends BasePage
{
  private static final String PROPRIETARY_API = "/rest/proprietary/";

  public ProprietaryComponentsRegressionPage() {
    super();
  }

  /**
   * Local matchers list container ({@code .local-proprietary-component-matchers}).
   * CSS class used as a structural anchor because the NxList container has no ARIA role or
   * accessible name; {@code getByRole(LIST)} would match every list on the page without a name
   * to scope it. The class is stable — it is set directly on the component root in the JSX.
   */
  public Locator localMatchersList() {
    return locator(".local-proprietary-component-matchers");
  }

  /**
   * Inherited matchers section ({@code .inherited-proprietary-component-matchers}).
   * Only present when the parent hierarchy has at least one matcher.
   * CSS class used as a structural anchor because the section container is a bare {@code <div>}
   * with no ARIA role or accessible name; the class is the only stable identifier on the element.
   */
  public Locator inheritedMatchersSection() {
    return locator(".inherited-proprietary-component-matchers");
  }

  /**
   * h3 heading inside the inherited section (text: {@code "Inherited from {parentName}"}).
   * Only valid when {@link #inheritedMatchersSection()} is visible.
   */
  public Locator inheritedSectionHeading() {
    return inheritedMatchersSection().getByRole(AriaRole.HEADING, withLevel(3));
  }

  /** Value text input in the add-matcher form row ({@code NxFormGroup label="Value"}). */
  public Locator valueInput() {
    return byLabel("Value");
  }

  /** Matcher Type select in the add-matcher form row ({@code NxFormGroup label="Matcher Type"}). */
  public Locator matcherTypeSelect() {
    return byLabel("Matcher Type");
  }

  /** All {@code <option>} elements inside the Matcher Type select, in document order. */
  public Locator matcherTypeOptions() {
    return matcherTypeSelect().locator("option");
  }

  /** "Add" button that appends the current value+type to the local matchers list. */
  public Locator addButton() {
    return byRole(AriaRole.BUTTON, "Add");
  }

  /** "Update" submit button (NxStatefulForm {@code submitBtnText="Update"}). */
  public Locator updateButton() {
    return byRole(AriaRole.BUTTON, "Update");
  }

  /**
   * NxStatefulForm validation errors alert ({@code role="alert" name="form validation errors"}).
   * Becomes visible after the user clicks Update with no changes (or with invalid input).
   */
  public Locator formValidationErrors() {
    return byRole(AriaRole.ALERT, "form validation errors");
  }

  /**
   * Single list row ({@code li}) in the local matchers list whose visible text contains
   * {@code matcherText}. Use to scope further assertions (text, subtext, delete button).
   */
  public Locator listRowForMatcher(String matcherText) {
    return localMatchersList()
        .locator("li")
        .filter(new Locator.FilterOptions().setHasText(matcherText));
  }

  /**
   * Subtext label inside the given matcher row (shows "Package" or "RegEx").
   * CSS class {@code .nx-list__subtext} used because the element is a bare {@code <span>} with
   * no ARIA role or accessible name; it is a presentation-only label rendered by the RSC
   * NxList component for which no semantic selector is available.
   */
  public Locator subtextInRow(Locator row) {
    return row.locator(".nx-list__subtext");
  }

  /**
   * Delete button (title="Delete", variant="icon-only") for the list item identified by its
   * visible matcher text. Scoped to the list row containing {@code matcherText} to avoid
   * ambiguity when multiple matchers are present.
   */
  public Locator deleteButtonForMatcher(String matcherText) {
    return listRowForMatcher(matcherText)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete"));
  }

  /**
   * Clicks Update and waits for the PUT response (HTTP 200) from the proprietary API.
   * Keeps the API URL out of the test body.
   */
  public void clickUpdateAndWaitForSave() {
    page.waitForResponse(
        response -> response.url().contains(PROPRIETARY_API) && response.status() == 200,
        () -> updateButton().click());
  }
}
