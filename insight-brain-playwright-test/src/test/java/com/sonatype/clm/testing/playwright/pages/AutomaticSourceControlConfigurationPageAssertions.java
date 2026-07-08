/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AutomaticSourceControlConfigurationPageAssertions
{
  private final AutomaticSourceControlConfigurationPage page;

  public AutomaticSourceControlConfigurationPageAssertions(AutomaticSourceControlConfigurationPage page) {
    this.page = page;
  }

  public void shouldRenderPageLayout() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
    assertThat(page.tile()).isVisible();
    assertThat(page.tileHeading()).isVisible();
    assertThat(page.enabledToggleLabel()).isVisible();
    assertThat(page.updateButton()).isVisible();
    assertThat(page.cancelButton()).isVisible();
  }

  public void shouldHaveEnabledToggleChecked() {
    assertThat(page.enabledToggleInput()).isChecked();
  }

  public void shouldHaveEnabledToggleUnchecked() {
    assertThat(page.enabledToggleInput()).not().isChecked();
  }

  public void shouldHaveCancelDisabled() {
    assertThat(page.cancelButton()).isDisabled();
  }

  public void shouldHaveCancelEnabled() {
    assertThat(page.cancelButton()).isEnabled();
  }
}
