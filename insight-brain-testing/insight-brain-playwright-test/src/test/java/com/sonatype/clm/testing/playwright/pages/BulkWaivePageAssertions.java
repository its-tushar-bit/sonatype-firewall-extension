/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class BulkWaivePageAssertions
{
  private final BulkWaivePage page;

  public BulkWaivePageAssertions(BulkWaivePage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
    assertThat(page.violationRows().first()).isVisible();
  }

  public void shouldHaveNextButtonDisabled() {
    assertThat(page.nextButton()).isDisabled();
  }

  public void shouldHaveNextButtonEnabled() {
    assertThat(page.nextButton()).isEnabled();
  }

  public void shouldShowSelectionCount(String expectedText) {
    assertThat(page.selectionCountLabel()).containsText(expectedText);
  }

  public void shouldShowEnterpriseBanner() {
    assertThat(page.enterpriseBanner()).isVisible();
  }

  public void shouldHaveBannerFlushTopClass() {
    assertThat(page.tile()).hasAttribute("class", Pattern.compile(".*iq-banner-flush-top.*"));
  }

  public void shouldShowViolationDetailsPopover() {
    assertThat(page.violationDetailsPopover()).isVisible();
  }

  public void shouldNotShowViolationDetailsPopover() {
    assertThat(page.violationDetailsPopover()).isHidden();
  }

  public void shouldShowReportFilterPopover() {
    assertThat(page.reportFilterPopover()).isVisible();
  }

  public void shouldShowNoResultsRow() {
    assertThat(page.noResultsRow()).isVisible();
  }

  public void shouldHaveFilterPlaceholder(Locator filterInput, String expectedPlaceholder) {
    assertThat(filterInput).hasAttribute("placeholder", expectedPlaceholder);
  }

  public void shouldShowEnterpriseBannerWithText(String expectedDescriptionSnippet) {
    assertThat(page.enterpriseBanner()).isVisible();
    assertThat(page.enterpriseBanner()).containsText(expectedDescriptionSnippet);
  }

  public void shouldShowConstraintOrComponentColumnHeader(String expectedText) {
    assertThat(page.constraintOrComponentColumnHeader()).hasText(expectedText);
  }

  public void shouldHaveThreatColumnSortDir(String expectedDir) {
    assertThat(page.threatColumnHeader()).hasAttribute("aria-sort", expectedDir);
  }

  public void shouldHaveSelectAllChecked(boolean expected) {
    if (expected) {
      assertThat(page.selectAllCheckboxInput()).isChecked();
    }
    else {
      assertThat(page.selectAllCheckboxInput()).not().isChecked();
    }
  }
}
