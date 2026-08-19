/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link WaiverDetailsPage}.
 */
public class WaiverDetailsPageAssertions
{
  private final WaiverDetailsPage page;

  public WaiverDetailsPageAssertions(WaiverDetailsPage page) {
    this.page = page;
  }

  public void shouldShowSidebarNav(String expectedTitle, int expectedItemCount) {
    assertThat(page.sidebarNavTitle()).containsText(expectedTitle);
    assertThat(page.sidebarNavItems()).hasCount(expectedItemCount);
  }

  public void shouldShowSidebarNavItem(int index, String expectedPolicyLabel, String expectedComponentName) {
    assertThat(page.sidebarNavItemPolicyName(index)).containsText(expectedPolicyLabel);
    assertThat(page.sidebarNavItemComponentName(index)).containsText(expectedComponentName);
  }

  public void shouldShowPageLayout(
      String expectedTileHeader,
      String expectedPolicy,
      String expectedConstraint,
      String expectedCondition,
      String expectedVulnerabilityButtonText,
      String expectedScope,
      String expectedReason,
      String expectedVersion,
      String expectedComponent,
      String expectedExpiration,
      String expectedComment,
      String expectedCreatedBy,
      String expectedDateCreated)
  {
    assertThat(page.detailsTileHeader()).containsText(expectedTileHeader);
    assertThat(page.detailsPolicy()).containsText(expectedPolicy);
    assertThat(page.detailsConstraint()).containsText(expectedConstraint);
    assertThat(page.detailsConditions()).containsText(expectedCondition);
    assertThat(page.vulnerabilityDetailsButton()).containsText(expectedVulnerabilityButtonText);
    assertThat(page.detailsScope()).containsText(expectedScope);
    assertThat(page.detailsReason()).containsText(expectedReason);
    assertThat(page.detailsVersion()).containsText(expectedVersion);
    assertThat(page.detailsComponent()).containsText(expectedComponent);
    assertThat(page.detailsExpiration()).containsText(expectedExpiration);
    assertThat(page.detailsComment()).containsText(expectedComment);
    assertThat(page.detailsCreatedBy()).containsText(expectedCreatedBy);
    assertThat(page.detailsDateCreated()).containsText(expectedDateCreated);
  }

  public void shouldShowPolicy(String expected) {
    assertThat(page.detailsPolicy()).containsText(expected);
  }

  public void shouldShowVulnerabilityDetailsModal() {
    assertThat(page.vulnerabilityDetailsModal()).isVisible();
  }

  public void shouldHideVulnerabilityDetailsModal() {
    assertThat(page.vulnerabilityDetailsModal()).isHidden();
  }

  public void shouldHideDeleteWaiverModal() {
    assertThat(page.deleteWaiverModal()).isHidden();
  }
}
