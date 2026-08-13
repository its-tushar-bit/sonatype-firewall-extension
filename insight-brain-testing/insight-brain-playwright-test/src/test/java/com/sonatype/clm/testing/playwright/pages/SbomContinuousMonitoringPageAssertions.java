/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SbomContinuousMonitoringPageAssertions
{
  private static final String ENABLED_LABEL = "Enabled";

  private static final String DISABLED_LABEL = "Disabled";

  private static final String NO_CHANGES_MESSAGE = "There are no changes to save.";

  private final SbomContinuousMonitoringPage page;

  public SbomContinuousMonitoringPageAssertions(SbomContinuousMonitoringPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.pageRoot()).isVisible();
    assertThat(page.pageHeading()).hasText("Continuous Monitoring");
  }

  public void shouldShowLearnMoreButton() {
    assertThat(page.learnMoreButton()).isVisible();
  }

  public void shouldShowUpdateButton() {
    assertThat(page.submitButton()).isVisible();
  }

  public void shouldShowToggle() {
    assertThat(page.toggleSwitch()).isAttached();
  }

  /** The switch's accessible name is computed from its label's visible content. */
  public void shouldShowToggleEnabledLabel() {
    assertThat(page.toggleSwitch()).hasAccessibleName(ENABLED_LABEL);
  }

  public void shouldShowToggleDisabledLabel() {
    assertThat(page.toggleSwitch()).hasAccessibleName(DISABLED_LABEL);
  }

  public void shouldShowToggleChecked() {
    assertThat(page.toggleSwitch()).isChecked();
  }

  public void shouldShowToggleNotChecked() {
    assertThat(page.toggleSwitch()).not().isChecked();
  }

  public void shouldShowNoChangesValidationError() {
    assertThat(page.noChangesValidationError()).containsText(NO_CHANGES_MESSAGE);
  }

  public void shouldNotShowNoChangesValidationError() {
    assertThat(page.noChangesValidationError()).not().isVisible();
  }
}
