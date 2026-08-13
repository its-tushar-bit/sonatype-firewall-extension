/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.Comparator;
import java.util.List;

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

  public void shouldShowNoViolationForComponentWithPolicy(String componentNameSubstring, String policyName) {
    assertThat(page.violationRowForComponentWithPolicy(componentNameSubstring, policyName)).hasCount(0);
  }

  public void shouldShowViolationCountForPolicy(int expectedCount, String policyName) {
    assertThat(page.violationRowsForPolicy(policyName)).hasCount(expectedCount);
  }

  public void shouldShowViolationRow(
      String componentNameSubstring,
      int expectedThreatLevel,
      String expectedPolicyName)
  {
    Locator row = page.violationRowForComponentWithPolicy(componentNameSubstring, expectedPolicyName).first();
    assertThat(row).isVisible();
    assertThat(page.violationRowThreatNumber(row)).hasText(String.valueOf(expectedThreatLevel));
    assertThat(page.violationRowPolicyName(row)).containsText(expectedPolicyName);
    assertThat(page.violationRowComponentName(row)).containsText(componentNameSubstring);
  }

  public void shouldShowNoPolicyViolations(String policyName) {
    assertThat(page.violationRowsForPolicy(policyName)).hasCount(0);
  }

  public void shouldShowWaivedIndicatorForComponentWithPolicy(
      String componentNameSubstring,
      String policyName)
  {
    Locator row = page.violationRowForComponentWithPolicy(componentNameSubstring, policyName).first();
    assertThat(row).isVisible();
    assertThat(page.violationRowWaivedIndicator(row)).isVisible();
  }

  public void shouldNotShowWaivedIndicatorForComponentWithPolicy(
      String componentNameSubstring,
      String policyName)
  {
    Locator row = page.violationRowForComponentWithPolicy(componentNameSubstring, policyName).first();
    assertThat(row).isVisible();
    assertThat(page.violationRowWaivedIndicator(row)).hasCount(0);
  }

  public void shouldShowViolationRowWithThreatCategory(
      String componentNameSubstring,
      int expectedThreatLevel,
      String expectedPolicyName,
      String expectedCategory)
  {
    Locator row = page.violationRowForComponentWithPolicy(componentNameSubstring, expectedPolicyName).first();
    assertThat(row).isVisible();
    assertThat(page.violationRowThreatNumber(row)).hasText(String.valueOf(expectedThreatLevel));
    assertThat(page.violationRowPolicyName(row)).containsText(expectedPolicyName);
    assertThat(page.violationRowComponentName(row)).containsText(componentNameSubstring);
    String expectedClass = "nx-threat-indicator--" + expectedCategory;
    assertThat(page.violationRowThreatIndicator(row)).hasClass(BasePage.cssClassPattern(expectedClass));
  }

  public void shouldShowViolationsSortedByThreatDescending() {
    assertThat(page.violationRows().first()).isVisible();
    // Single atomic `allInnerTexts()` round-trip — avoids `count()`+`nth(i)` TOCTOU races
    // where the row collection can change between the two calls during a re-sort animation.
    List<Integer> threatNums = page.violationRowThreatNumbers()
        .allInnerTexts()
        .stream()
        .map(String::trim)
        .map(text -> {
          Assertions.assertThat(text)
              .as("threat-number cell text should be numeric — got '%s'", text)
              .matches("^\\d+$");
          return Integer.parseInt(text);
        })
        .toList();
    Assertions.assertThat(threatNums)
        .as("violations should be sorted by descending threat level")
        .isSortedAccordingTo(Comparator.reverseOrder());
  }
}
