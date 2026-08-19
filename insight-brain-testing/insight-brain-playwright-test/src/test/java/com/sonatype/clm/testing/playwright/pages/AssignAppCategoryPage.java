/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

/**
 * Playwright page object for the Assign Application Categories page.
 * <p>
 * URL: {@code …/management/view/application/{appPublicId}/category}<br>
 * Route state: {@code management.edit.application.category}<br>
 * Page container id: {@code application-category-editor}.
 * <p>
 * Renders an {@code IqAssociationEditor} with {@code NxCheckbox} items for each applicable
 * category. Submitting with {@code submitBtnText="Update"} saves the selection via
 * {@code PUT /rest/appliedTag/application/{appPublicId}}.
 */
public class AssignAppCategoryPage
    extends BasePage
{
  public AssignAppCategoryPage() {
    super();
  }

  /** Root container of the Assign Application Categories page. */
  public Locator container() {
    return locator("#application-category-editor");
  }

  /** Level-1 page heading: "Assign Application Categories". */
  public Locator heading() {
    return container().locator("h1");
  }

  /**
   * Checkbox for the given category name in the {@code IqAssociationEditor}.
   * {@code NxCheckbox} renders as a {@code role="checkbox"} element whose accessible name
   * is the category name span text.
   */
  public Locator categoryCheckbox(String categoryName) {
    return container().getByRole(AriaRole.CHECKBOX,
        new Locator.GetByRoleOptions().setName(categoryName));
  }

  /** Submit ("Update") button of the {@code NxStatefulForm}, scoped to the page container. */
  public Locator submitButton() {
    return container().locator(".nx-form__submit-btn");
  }

  /**
   * Clicks the label element for the given category name to toggle its checkbox.
   * RSC {@code NxCheckbox} renders a visually-hidden {@code <input>} inside a {@code <label>};
   * clicking the label (not the input) is required to trigger the toggle.
   */
  public void checkCategory(String categoryName) {
    container()
        .locator("label")
        .filter(new Locator.FilterOptions().setHasText(categoryName))
        .click();
  }

  /** Clicks the "Update" submit button. */
  public void submit() {
    submitButton().click();
  }

  /**
   * Waits for the {@code NxSubmitMask} success overlay to appear then auto-dismiss.
   * The SPA stays on the assign-categories route after a successful save — no navigation occurs.
   */
  public void waitForSaveSuccess() {
    Locator successMask = page.locator(".nx-submit-mask--success");
    successMask.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.VISIBLE)
        .setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
    successMask.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN)
        .setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
  }
}
