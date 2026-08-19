/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class DashboardWaiversComponentAssertions
{
  private final DashboardWaiversComponent page;

  public DashboardWaiversComponentAssertions(DashboardWaiversComponent page) {
    this.page = page;
  }

  public void shouldHaveWaiverCount(int expectedCount) {
    assertThat(page.waivers()).hasCount(expectedCount);
  }

  public void shouldShowNoDataMessage(String expectedText) {
    assertThat(page.noDataMessage()).containsText(expectedText);
  }

  public void shouldShowThreatLevel(int rowIndex, String expectedThreatLevel) {
    assertThat(page.threatNumber(rowIndex)).containsText(expectedThreatLevel);
  }

  public void shouldShowCreateTime(int rowIndex, String expectedDate) {
    assertThat(page.createTime(rowIndex)).containsText(expectedDate);
  }

  public void shouldShowExpiryTime(int rowIndex, String expectedExpiry) {
    assertThat(page.expiryTime(rowIndex)).containsText(expectedExpiry);
  }

  public void shouldShowPolicy(int rowIndex, String expectedPolicyName) {
    assertThat(page.policy(rowIndex)).containsText(expectedPolicyName);
  }

  public void shouldShowScope(int rowIndex, String expectedScope) {
    assertThat(page.scope(rowIndex)).containsText(expectedScope);
  }

  public void shouldShowComponent(int rowIndex, String expectedComponent) {
    assertThat(page.component(rowIndex)).containsText(expectedComponent);
  }

  public void shouldShowUpgradeAvailable(int rowIndex, String expectedText) {
    assertThat(page.upgradeAvailable(rowIndex)).containsText(expectedText);
  }

  public void shouldShowExistingWaiversTab() {
    assertThat(page.existingWaiversTab()).isVisible();
  }

  public void shouldShowRequestedWaiversTab() {
    assertThat(page.requestedWaiversTab()).isVisible();
  }

  public void shouldShowWaiverRequestsTable() {
    assertThat(page.waiverRequestsTable()).isVisible();
  }
}
