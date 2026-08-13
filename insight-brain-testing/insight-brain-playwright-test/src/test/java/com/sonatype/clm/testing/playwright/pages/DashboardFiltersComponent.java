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
 * Playwright page object for the Dashboard filter drawer.
 */
public class DashboardFiltersComponent
    extends BasePage
{
  private static final String CONTAINER = "#dashboard-filter-container";

  public DashboardFiltersComponent() {
    super();
  }

  public Locator filterContainer() {
    return locator(CONTAINER);
  }

  public Locator closeButton() {
    return byRole(AriaRole.BUTTON, "Close");
  }

  public Locator applyButton() {
    return byRole(AriaRole.BUTTON, "Apply");
  }

  public Locator revertButton() {
    return byRole(AriaRole.BUTTON, "Revert");
  }

  public Locator saveButton() {
    return byRole(AriaRole.BUTTON, "Save");
  }

  // --------------- Filter sections ---------------

  public Locator organizationFilter() {
    return locator("#org-app-filters > div:nth-child(1)");
  }

  public Locator applicationFilter() {
    return locator("#org-app-filters > div:nth-child(2)");
  }

  public Locator stageFilter() {
    return locator("#stage-filter");
  }

  public Locator policyThreatLevelFilter() {
    return locator("#threat-level-filter");
  }

  public Locator policyWaiverReasonFilter() {
    return locator("#policy-waiver-reason-filter");
  }

  public Locator repositoryFilter() {
    return locator("#repositories-filter");
  }

  public Locator categoryFilter() {
    return locator("#category-filter");
  }

  public Locator policyTypeFilter() {
    return locator("#policy-type-filter");
  }

  public Locator ageFilter() {
    return locator("#age-filter");
  }

  public Locator expirationDateFilter() {
    return locator("#expiration-date-filter");
  }

  // --------------- Filter section child helpers ---------------

  public Locator twisty(Locator filterSection) {
    return filterSection.locator(".nx-collapsible-items__trigger");
  }

  public Locator allItemsCheckbox(Locator filterSection) {
    return filterSection.locator(".nx-collapsible-items__children .nx-collapsible-items__child:first-child");
  }

  public Locator checkboxItem(Locator filterSection, int index) {
    return filterSection
        .locator(".nx-collapsible-items__children .nx-collapsible-items__child:nth-child(" + index + ")");
  }

  // --------------- Policy threat level slider ---------------

  /**
   * Returns the two MUI slider thumb wrappers (min, max) inside the policy-threat-level filter.
   * The threat-level filter is rendered as an NxPolicyThreatSlider, which wraps an MUI
   * {@code <Slider>}. Each visible thumb is a {@code .MuiSlider-thumb} span containing a hidden
   * {@code <input type="range">} that handles keyboard input ({@code Home}/{@code End}/arrows).
   */
  public Locator policyThreatLevelSliderThumbs() {
    return policyThreatLevelFilter().locator(".MuiSlider-thumb");
  }

  /**
   * Sets the policy-threat-level filter range to [min, max] (each in 0..10) using keyboard control
   * of the MUI slider thumbs. This is the Playwright equivalent of the legacy Selenide
   * {@code NxThreatLevelSlider.setValues(min, max)}.
   *
   * <p>
   * The threat-level filter section must already be expanded (via
   * {@link #twisty(Locator)}.click()) before calling this — NxCollapsibleItems does not render
   * its children until expanded.
   *
   * <p>
   * The dashboard default is [2, 10]; calling this with different values triggers a filter
   * state change so that {@link #applyButton()} becomes enabled.
   */
  public void setPolicyThreatLevelRange(int min, int max) {
    if (min < 0 || max > 10 || min > max) {
      throw new IllegalArgumentException("Invalid threat level range [" + min + ", " + max + "]");
    }
    Locator thumbs = policyThreatLevelSliderThumbs();
    assertThat(thumbs.first())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));

    // The actual focusable/keyboard-handling element is the hidden <input type="range"> inside
    // each .MuiSlider-thumb wrapper. Focus it and drive with keyboard.
    Locator leftInput = thumbs.first().locator("input");
    leftInput.focus();
    leftInput.press("Home");
    for (int i = 0; i < min; i++) {
      leftInput.press("ArrowRight");
    }

    Locator rightInput = thumbs.last().locator("input");
    rightInput.focus();
    rightInput.press("End");
    for (int i = 0; i < (10 - max); i++) {
      rightInput.press("ArrowLeft");
    }
  }

  // --------------- Waiver-reason filter helpers ---------------

  /**
   * Expand the policy-waiver-reason filter section. The section must already be present
   * (filter drawer expanded), otherwise the click target won't render.
   */
  public void expandWaiverReasonFilter() {
    assertThat(policyWaiverReasonFilter()).isVisible();
    twisty(policyWaiverReasonFilter()).click();
  }

  // --------------- Actions ---------------

  public void apply() {
    applyButton().click();
    waitForVisibleThenHidden(".nx-submit-mask");
  }

  public void closeFilter() {
    closeButton().click();
    assertThat(filterContainer())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
  }

}
