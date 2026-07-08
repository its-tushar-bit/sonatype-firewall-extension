/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AutomaticApplicationsConfigurationPageAssertions
{
  private final AutomaticApplicationsConfigurationPage page;

  public AutomaticApplicationsConfigurationPageAssertions(AutomaticApplicationsConfigurationPage page) {
    this.page = page;
  }

  public void shouldRenderPageLayout() {
    assertThat(page.pageHeading()).isVisible();
    assertThat(page.tile()).isVisible();
    assertThat(page.tileHeading()).isVisible();
    assertThat(page.enabledToggleLabel()).isVisible();
    assertThat(page.parentOrganizationSelect()).isVisible();
    assertThat(page.updateButton()).isVisible();
    assertThat(page.cancelButton()).isVisible();
  }

  public void shouldHaveEnabledToggleChecked() {
    assertThat(page.enabledToggleInput()).isChecked();
  }

  public void shouldHaveEnabledToggleUnchecked() {
    assertThat(page.enabledToggleInput()).not().isChecked();
  }

  public void shouldHaveParentOrgSelectEnabled() {
    assertThat(page.parentOrganizationSelect()).isEnabled();
  }

  public void shouldHaveParentOrgSelectDisabled() {
    assertThat(page.parentOrganizationSelect()).isDisabled();
  }

  public void shouldHaveUpdateButtonDisabled() {
    assertThat(page.updateButton()).isDisabled();
  }

  public void shouldHaveUpdateButtonEnabled() {
    assertThat(page.updateButton()).isEnabled();
  }

  public void shouldHaveCancelDisabled() {
    assertThat(page.cancelButton()).isDisabled();
  }

  public void shouldHaveCancelEnabled() {
    assertThat(page.cancelButton()).isEnabled();
  }

  public void shouldShowExplanatoryLinksToScmConfigAndOnboarding() {
    assertThat(page.sourceControlExplanation()).isVisible();
    assertThat(page.automaticSourceControlLink()).isVisible();
    assertThat(page.scmOnboardingExplanation()).isVisible();
    assertThat(page.scmOnboardingLink()).isVisible();
  }
}
