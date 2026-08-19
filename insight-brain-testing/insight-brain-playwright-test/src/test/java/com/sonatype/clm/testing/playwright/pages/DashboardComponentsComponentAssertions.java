/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class DashboardComponentsComponentAssertions
{
  private final DashboardComponentsComponent page;

  public DashboardComponentsComponentAssertions(DashboardComponentsComponent page) {
    this.page = page;
  }

  public void shouldShowNoDataMessage(String expectedText) {
    assertThat(page.noDataMessage()).containsText(expectedText);
  }

  public void shouldHaveCount(int expected) {
    assertThat(page.components()).hasCount(expected);
  }

  public void shouldShowComponentRow(int index, String componentName) {
    assertThat(page.componentName(index)).containsText(componentName);
  }

  public void shouldShowComponentRiskDetail(String componentName) {
    assertThat(page.dashboardContainer()).isHidden();
    assertThat(page.componentRiskRoot()).isVisible();
    page.componentRiskHeading()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    assertThat(page.componentRiskHeading()).containsText(componentName);
  }
}
