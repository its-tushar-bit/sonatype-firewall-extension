/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Assertion helpers for {@link SonatypeDeveloperPage}. */
public class SonatypeDeveloperPageAssertions
{
  private final SonatypeDeveloperPage page;

  public SonatypeDeveloperPageAssertions(SonatypeDeveloperPage page) {
    this.page = page;
  }

  public void shouldShowContainer() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowPageTitle(String expectedTitle) {
    assertThat(page.headingByName(expectedTitle)).isVisible();
  }

  public void shouldShowLicenseLockScreen() {
    assertThat(page.lockScreenErrorAlert()).isVisible();
    assertThat(page.lockScreenErrorAlert()).hasText(SonatypeDeveloperPage.LOCK_SCREEN_ERROR_TEXT);
  }

  public void shouldShowSummaryTableSection() {
    assertThat(page.summaryHeading()).isVisible();
    assertThat(page.summaryFilterButton()).isVisible();
  }

  public void shouldShowSummaryDisabledInfoAlert() {
    assertThat(page.summaryDisabledInfoAlert()).isVisible();
    assertThat(page.summaryDisabledInfoAlert())
        .containsText(SonatypeDeveloperPage.SUMMARY_DISABLED_ALERT_TEXT);
  }

  public void shouldShowAllIntegrationCards() {
    assertThat(page.allIntegrationCards()).hasCount(3);
    assertThat(page.integrationCard(SonatypeDeveloperPage.CI_CARD_NAME)).isVisible();
    assertThat(page.integrationCard(SonatypeDeveloperPage.SCM_CARD_NAME)).isVisible();
    assertThat(page.integrationCard(SonatypeDeveloperPage.IDE_CARD_NAME)).isVisible();
  }
}
