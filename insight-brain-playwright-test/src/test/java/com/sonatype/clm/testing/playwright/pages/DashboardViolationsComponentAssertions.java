/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link DashboardViolationsComponent}.
 */
public class DashboardViolationsComponentAssertions
{
  private final DashboardViolationsComponent page;

  public DashboardViolationsComponentAssertions(DashboardViolationsComponent page) {
    this.page = page;
  }

  public void shouldShowNoDataMessage(String expectedText) {
    assertThat(page.noDataMessage()).containsText(expectedText);
  }

  public void shouldHaveCount(int expected) {
    assertThat(page.violations()).hasCount(expected);
  }

  public void shouldShowViolationRow(int index, String componentArtifactId, String policyName, String appName) {
    assertThat(page.componentName(index)).containsText(componentArtifactId);
    assertThat(page.policyName(index)).containsText(policyName);
    assertThat(page.applicationName(index)).containsText(appName);
  }
}
