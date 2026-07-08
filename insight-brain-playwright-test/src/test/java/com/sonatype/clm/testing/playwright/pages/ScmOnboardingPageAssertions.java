/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ScmOnboardingPageAssertions
{
  private final ScmOnboardingPage page;

  public ScmOnboardingPageAssertions(ScmOnboardingPage page) {
    this.page = page;
  }

  public void shouldShowContainer() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowNewOrganizationButton() {
    assertThat(page.newOrganizationButton()).isVisible();
  }

  public void shouldShowTargetOrganizationDropdown() {
    assertThat(page.targetOrganizationDropdown()).isVisible();
  }

  public void shouldShowScmTokenNotConfiguredError() {
    assertThat(page.scmTokenNotConfiguredError()).isVisible();
  }
}
