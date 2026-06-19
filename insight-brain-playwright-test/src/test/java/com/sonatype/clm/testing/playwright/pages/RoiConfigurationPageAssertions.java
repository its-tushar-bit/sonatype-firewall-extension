/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RoiConfigurationPageAssertions
{
  private final RoiConfigurationPage page;

  public RoiConfigurationPageAssertions(RoiConfigurationPage page) {
    this.page = page;
  }

  public void shouldRenderPageLayout() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
    assertThat(page.tileHeading()).isVisible();
    assertThat(page.editLink()).isVisible();
  }

  public void shouldShowBaselineDaysContaining(String expectedDigits) {
    assertThat(page.baselineDaysValue()).containsText(expectedDigits);
  }

  public void shouldShowDailyRiskContaining(String expectedDigits) {
    assertThat(page.dailyRiskValue()).containsText(expectedDigits);
  }
}
