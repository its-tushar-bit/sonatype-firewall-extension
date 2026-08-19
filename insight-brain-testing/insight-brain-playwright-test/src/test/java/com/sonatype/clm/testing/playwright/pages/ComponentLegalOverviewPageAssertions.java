/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ComponentLegalOverviewPageAssertions
{
  private final ComponentLegalOverviewPage page;

  public ComponentLegalOverviewPageAssertions(ComponentLegalOverviewPage page) {
    this.page = page;
  }

  public void shouldBeLoaded() {
    assertThat(page.container()).isVisible();
  }

  public void shouldHaveObligationCount(int expectedCount) {
    assertThat(page.obligationRows()).hasCount(expectedCount);
  }

  public void shouldShowAllObligationStatus(int count, String expectedStatus) {
    for (int i = 0; i < count; i++) {
      assertThat(page.obligationStatusAt(i)).hasText(expectedStatus);
    }
  }

  public void shouldHideResolveAllButton() {
    assertThat(page.resolveAllButton()).isHidden();
  }

  public void shouldHaveExpandedAccordionCount(int expectedCount) {
    assertThat(page.attributionAccordions()).hasCount(expectedCount);
    for (int i = 0; i < expectedCount; i++) {
      Locator accordion = page.attributionAccordions().nth(i);
      assertThat(accordion).hasAttribute("aria-expanded", "true");
    }
  }

  public void shouldShowLicenseObligationsTile() {
    assertThat(page.licenseObligationsTile()).isVisible();
  }

  public void shouldShowAttributionSummaryTile() {
    assertThat(page.attributionSummaryTile()).isVisible();
  }

  public void shouldShowSegmentedButtonDropdownMenu(int obligationIndex) {
    assertThat(page.segmentedButtonDropdownMenu(obligationIndex)).isVisible();
  }

  public void shouldHideSegmentedButtonDropdownMenu(int obligationIndex) {
    assertThat(page.segmentedButtonDropdownMenu(obligationIndex)).isHidden();
  }

  public void shouldHaveDropdownOptionCount(int obligationIndex, int expectedCount) {
    assertThat(page.segmentedButtonDropdownOptions(obligationIndex)).hasCount(expectedCount);
  }

  public void shouldHaveObligationAccordionCollapsed(int index) {
    assertThat(page.obligationAccordionAt(index)).hasAttribute("aria-expanded", "false");
  }

  public void shouldHaveObligationHeaderCountText(int index, String expectedText) {
    assertThat(page.obligationHeaderCountText(index)).containsText(expectedText);
  }

  public void shouldShowFulfilledStatusIcon() {
    assertThat(page.obligationStatusIconFulfilled().first()).isVisible();
  }

  public void shouldShowAdditionalAttributionTile() {
    assertThat(page.additionalAttributionTile()).isVisible();
  }

  public void shouldShowAdditionalAttributionEditButton() {
    assertThat(page.additionalAttributionEditButton()).isVisible();
  }

  public void shouldShowAdditionalAttributionModal() {
    assertThat(page.additionalAttributionModal()).isVisible();
  }

  public void shouldHideAdditionalAttributionModal() {
    assertThat(page.additionalAttributionModal()).isHidden();
  }
}
