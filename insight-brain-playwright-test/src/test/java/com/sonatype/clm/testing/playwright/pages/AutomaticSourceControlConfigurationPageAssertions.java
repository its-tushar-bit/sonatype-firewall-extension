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

  public void shouldRenderFormLayout() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
    assertThat(page.toggleLabel()).isVisible();
    assertThat(page.updateButton()).isVisible();
    assertThat(page.cancelButton()).isVisible();
  }

  public void shouldHaveToggleChecked() {
    assertThat(page.toggleInput()).isChecked();
  }

  public void shouldHaveToggleUnchecked() {
    assertThat(page.toggleInput()).not().isChecked();
  }

  public void shouldHaveCancelButtonDisabled() {
    assertThat(page.cancelButton()).isDisabled();
  }

  public void shouldHaveCancelButtonEnabled() {
    assertThat(page.cancelButton()).isEnabled();
  }
}
