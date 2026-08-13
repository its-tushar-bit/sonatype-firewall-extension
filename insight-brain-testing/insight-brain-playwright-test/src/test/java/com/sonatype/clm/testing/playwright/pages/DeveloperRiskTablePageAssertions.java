/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class DeveloperRiskTablePageAssertions
{
  private final DeveloperRiskTablePage page;

  public DeveloperRiskTablePageAssertions(DeveloperRiskTablePage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowRowForApp(String applicationName) {
    assertThat(page.rowByAppName(applicationName)).isVisible();
  }

  public void shouldShowApplicationLinkInRow(Locator row, String hrefSubstring) {
    Locator link = page.applicationLinkInRow(row);
    assertThat(link).isVisible();
    // Substring match on the href fragment. Use {@link BasePage#escapeForJsRegex} (NOT
    // {@code Pattern.quote()}) because Playwright Java serialises the pattern to a JS RegExp,
    // which does not understand Java's {@code \Q…\E} quoting.
    assertThat(link).hasAttribute("href", Pattern.compile(BasePage.escapeForJsRegex(hrefSubstring)));
  }

  public void shouldShowCiCdConfigureButtonInRow(Locator row) {
    assertThat(page.cicdConfigureButtonInRow(row)).isVisible();
  }

  public void shouldShowScmConfigureButtonInRow(Locator row) {
    assertThat(page.scmConfigureButtonInRow(row)).isVisible();
  }

  public void shouldShowNoneInDateColumnsForRow(Locator row) {
    // {@code formatTimestampToDate} returns the literal "None" when the timestamp is 0,
    // which is the default for an app with no commits / no evaluations yet.
    // Scope to the two specific date cells (Last Commit, Last Evaluation) with exact text
    // matching; a substring count of "None" anywhere in the row would mask regressions
    // where one date cell renders wrong while another tooltip/cell adds "None".
    assertThat(page.lastCommitDateCellInRow(row))
        .hasText("None", new com.microsoft.playwright.assertions.LocatorAssertions.HasTextOptions()
            .setUseInnerText(true));
    assertThat(page.lastEvaluationDateCellInRow(row))
        .hasText("None", new com.microsoft.playwright.assertions.LocatorAssertions.HasTextOptions()
            .setUseInnerText(true));
  }

  public void shouldShowNAInPrioritiesColumnForRow(Locator row) {
    assertThat(row.getByText("N/A")).isVisible();
  }

  public void shouldShowEmptyState() {
    assertThat(page.emptyStateCell()).isVisible();
  }

  public void shouldShowFilterPopover() {
    assertThat(page.filterPopover()).isVisible();
    assertThat(page.filterApplyButton()).isVisible();
  }

  public void shouldShowFilterPopoverFieldsets() {
    assertThat(page.filterPopover().getByText("CI/CD Configuration")).isVisible();
    assertThat(page.filterPopover().getByText("SCM Feedback Configuration")).isVisible();
  }

  public void shouldNotShowFilterPopover() {
    assertThat(page.filterPopover()).hasCount(0);
  }
}
