/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link EnterpriseReportingPage}.
 */
public class EnterpriseReportingPageAssertions
{
  private final EnterpriseReportingPage page;

  public EnterpriseReportingPageAssertions(EnterpriseReportingPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
  }

  public void shouldHaveHeading(String expectedHeading) {
    assertThat(page.pageHeading()).hasText(expectedHeading);
  }

  public void shouldShowEnterpriseDashboardsSectionHeading(String expectedTitle) {
    // h2 text includes a sibling description paragraph; containsText avoids coupling to marketing copy.
    assertThat(page.enterpriseDashboardsSectionHeading()).containsText(expectedTitle);
  }

  public void shouldShowEnterpriseDashboardCardWithTitle(String dashboardId, String expectedTitle) {
    assertThat(page.enterpriseDashboardCard(dashboardId)).isVisible();
    assertThat(page.enterpriseDashboardCard(dashboardId).locator("h3")).hasText(expectedTitle);
  }

  public void shouldShowDashboardSubpage() {
    assertThat(page.dashboardPageContainer()).isVisible();
    assertThat(page.dashboardIframeContainer()).isVisible();
  }

  public void shouldShowDashboardSubpageWithTitle(String expectedTitle) {
    assertThat(page.dashboardSubpageHeading()).hasText(expectedTitle);
  }

  public void shouldShowReact2ShellCard() {
    assertThat(page.react2ShellCard()).isVisible();
    assertThat(page.react2ShellCardHeading()).hasText("React2Shell Impact");
    assertThat(page.react2ShellViewLink()).isVisible();
  }

  public void shouldShowSupportInfoSection() {
    assertThat(page.supportInfoSection()).isVisible();
  }

  public void shouldShowCopySupportInfoButton() {
    assertThat(page.copySupportInfoButton()).isVisible();
  }

  public void shouldShowCheckmarkIcon() {
    assertThat(page.copySupportInfoIcon()).hasClass(COPIED_CLASS_PATTERN);
  }

  /** {@code \b} + {@code .*} are JS-RegExp-safe; do not add {@code \Q…\E} or {@code (?i)}. */
  private static final Pattern COPIED_CLASS_PATTERN = Pattern.compile(".*\\bcopied\\b.*");

  public void shouldShowCopyConfirmationMessage() {
    assertThat(page.copyConfirmationMessage()).isVisible();
    assertThat(page.copyConfirmationMessage()).hasText(EnterpriseReportingPage.COPY_CONFIRMATION_MESSAGE);
  }

  public void shouldShowSupportInfoLoadError() {
    assertThat(page.supportInfoLoadError()).isVisible();
    assertThat(page.supportInfoLoadErrorRetryButton()).isVisible();
  }
}
