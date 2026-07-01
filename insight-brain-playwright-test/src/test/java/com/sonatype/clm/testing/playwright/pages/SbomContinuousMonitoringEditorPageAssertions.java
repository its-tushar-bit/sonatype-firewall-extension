/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SbomContinuousMonitoringEditorPageAssertions
{
  private final SbomContinuousMonitoringEditorPage page;

  public SbomContinuousMonitoringEditorPageAssertions(SbomContinuousMonitoringEditorPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
    assertThat(page.title()).isVisible();
  }

  public void shouldShowToggleAndUpdateControls() {
    assertThat(page.enableToggle().label()).isVisible();
    assertThat(page.updateButton()).isVisible();
  }

  public void shouldShowLearnMoreButton() {
    assertThat(page.learnMoreButton()).isVisible();
  }

  public void shouldHaveToggleEnabled() {
    assertThat(page.enableToggle().input()).isEnabled();
  }

  public void shouldHaveToggleDisabled() {
    assertThat(page.enableToggle().input()).isDisabled();
  }

  public void shouldHaveToggleChecked() {
    assertThat(page.enableToggle().input()).isChecked();
  }

  public void shouldHaveToggleUnchecked() {
    assertThat(page.enableToggle().input()).not().isChecked();
  }

  public void shouldHaveUpdateButtonEnabled() {
    assertThat(page.updateButton()).isEnabled();
  }

  public void shouldHaveUpdateButtonDisabled() {
    assertThat(page.updateButton()).isDisabled();
  }

  /**
   * Pristine form ({@code isDirty == false}) → NxStatefulForm sets
   * {@code validationErrors = MSG_NO_CHANGES_TO_SAVE}, which adds
   * {@code nx-form--has-validation-errors} to the parent {@code <form>}. The role=alert banner
   * is in the DOM but CSS-hidden pre-submit, so we assert on the class-bearing form instead.
   */
  public void shouldShowNoChangesToSaveIndicator() {
    assertThat(page.formWithNoChangesValidationErrorClass()).hasCount(1);
  }

  /** Dirty form ({@code isDirty == true}) → {@code validationErrors} is undefined → class absent. */
  public void shouldShowChangesPendingIndicator() {
    assertThat(page.formWithNoChangesValidationErrorClass()).hasCount(0);
  }
}
