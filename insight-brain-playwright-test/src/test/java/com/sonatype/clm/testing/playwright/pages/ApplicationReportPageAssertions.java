/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import com.microsoft.playwright.Locator;

import org.assertj.core.api.Assertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ApplicationReportPageAssertions
{
  private final ApplicationReportPage page;

  public ApplicationReportPageAssertions(ApplicationReportPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.appReportMain()).isVisible();
  }

  public void shouldShowReportHeaderContaining(String text) {
    assertThat(page.appReportMain()).isVisible();
    assertThat(page.reportHeaderTitle()).containsText(text);
  }

  public void shouldShowBackButtonWithText(String expectedText) {
    assertThat(page.backButton()).isVisible();
    assertThat(page.backButton()).hasText(expectedText);
  }

  public void shouldShowUnscannableAlert(String expectedText) {
    assertThat(page.unscannableComponentsAlert()).isVisible();
    assertThat(page.unscannableComponentsAlert()).containsText(expectedText);
  }

  public void shouldShowUnscannedComponentsModal(String expectedHeader) {
    assertThat(page.unscannedComponentsModal()).isVisible();
    assertThat(page.unscannedComponentsModalHeader()).hasText(expectedHeader);
  }

  public void shouldShowReevaluationErrorWithoutModal(String expectedText) {
    assertThat(page.reevaluationErrorAlert()).isVisible();
    assertThat(page.reevaluationErrorAlert()).containsText(expectedText);
    assertThat(page.reevaluationStatusModal()).not().isVisible();
  }

  public void shouldShowNavigationControls() {
    assertThat(page.backButton()).isVisible();
    assertThat(page.optionsDropdown()).isVisible();
    assertThat(page.viewDependencyTreeButton()).isVisible();
  }

  public void shouldShowOptionsDropdownLinks() {
    page.optionsDropdown().click();
    assertThat(page.viewVulnerabilitiesLink()).isVisible();
    assertThat(page.viewRawDataLink()).isVisible();
  }

  public void shouldShowViolationRows() {
    assertThat(page.violationRows().first()).isVisible();
  }

  public void shouldShowWaivedViolationsIndicator() {
    assertThat(page.firstWaivedViolationsIndicator()).isVisible();
  }

  public void shouldShowViolationsSortedByThreatDescending() {
    Locator violationRows = page.violationRows();
    assertThat(violationRows.first()).isVisible();
    List<Integer> threatNums = IntStream.range(0, violationRows.count())
        .mapToObj(i -> {
          String text = page.violationRowThreatNumber(violationRows.nth(i)).innerText().trim();
          Assertions.assertThat(text)
              .as("threat number in row %d should be a digit", i)
              .matches("^\\d+$");
          return Integer.parseInt(text);
        })
        .toList();
    Assertions.assertThat(threatNums)
        .as("violations should be sorted by descending threat level")
        .isSortedAccordingTo(Comparator.reverseOrder());
  }
}
