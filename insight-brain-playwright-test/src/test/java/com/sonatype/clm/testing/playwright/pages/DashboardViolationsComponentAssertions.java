/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

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

  /**
   * Web-first lower-bound assertion: auto-retries until the {@code expected}-th row mounts.
   * Use for dashboard queries that span the whole tenant — exact counts are flaky because
   * sibling tests may leave root-org violations that inflate the row count.
   */
  public void shouldHaveAtLeastCount(int expected) {
    assertThat(page.violations().nth(expected - 1)).isVisible();
  }

  public void shouldShowViolationRow(int index, String componentArtifactId, String policyName, String appName) {
    assertThat(page.componentName(index)).containsText(componentArtifactId);
    assertThat(page.policyName(index)).containsText(policyName);
    assertThat(page.applicationName(index)).containsText(appName);
  }

  public void shouldShowExpectedColumns() {
    assertThat(page.allHeaders()).hasCount(6);
    assertThat(page.allHeaders().nth(0)).containsText("Threat");
    assertThat(page.allHeaders().nth(1)).containsText("Policy");
    assertThat(page.allHeaders().nth(2)).containsText("Application");
    assertThat(page.allHeaders().nth(3)).containsText("Component");
    assertThat(page.allHeaders().nth(4)).containsText("Age");
    assertThat(page.allHeaders().nth(5)).containsText("Select Row");
  }

  public void shouldHaveSortState(DashboardViolationsComponent.SortableColumn column, String ariaSortValue) {
    assertThat(page.headerCell(column)).hasAttribute("aria-sort", ariaSortValue);
  }

  public void assertPaginationFirstPageState() {
    assertThat(page.paginatorBar()).isVisible();
    assertThat(page.paginatorNextButton()).isVisible();
    assertThat(page.paginatorPreviousButton()).isHidden();
  }

  public void assertPaginationAfterNextClick() {
    assertThat(page.paginatorPreviousButton()).isVisible();
  }

  public void assertPaginationReturnedToFirstPageState() {
    assertThat(page.paginatorNextButton()).isVisible();
    assertThat(page.paginatorPreviousButton()).isHidden();
  }

  public void assertNoPaginator() {
    assertThat(page.paginatorBar()).isHidden();
  }
}
