/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WaiverConfigurationPageAssertions
{
  private final WaiverConfigurationPage page;

  public WaiverConfigurationPageAssertions(WaiverConfigurationPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    page.container().waitFor();
    assertThat(page.container()).isVisible();
  }

  public void shouldHaveNextButtonDisabled() {
    assertThat(page.nextButton()).isDisabled();
  }

  public void shouldHaveNextButtonEnabled() {
    assertThat(page.nextButton()).isEnabled();
  }

  public void shouldShowEnterpriseBanner() {
    assertThat(page.enterpriseBanner()).isVisible();
  }

  public void shouldShowEnterpriseBannerWithText(String expectedDescriptionSnippet) {
    assertThat(page.enterpriseBanner()).isVisible();
    assertThat(page.enterpriseBanner()).containsText(expectedDescriptionSnippet);
  }

  public void shouldShowAllFormFields() {
    assertThat(page.scopeDropdown()).isVisible();
    assertThat(page.exactComponentRadio()).isVisible();
    assertThat(page.allVersionsRadio()).isVisible();
    assertThat(page.expirySelect()).isVisible();
    assertThat(page.reasonSelect()).isVisible();
    assertThat(page.commentsTextarea()).isVisible();
  }
}
