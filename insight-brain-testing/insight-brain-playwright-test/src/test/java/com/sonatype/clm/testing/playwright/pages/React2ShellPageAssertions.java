/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class React2ShellPageAssertions
{
  private final React2ShellPage page;

  public React2ShellPageAssertions(React2ShellPage page) {
    this.page = page;
  }

  public void shouldShowPageChrome() {
    assertThat(page.container()).isVisible();
    assertThat(page.pageHeading()).isVisible();
  }

  public void shouldShowImpactSummary() {
    assertThat(page.impactSummaryHeading()).isVisible();
    assertThat(page.summaryTileTitle("Affected Applications")).isVisible();
    assertThat(page.summaryTileValue("Affected Applications")).containsText("1");
    assertThat(page.summaryTileTitle("Affected Components")).isVisible();
    assertThat(page.summaryTileValue("Affected Components")).containsText("1");
    assertThat(page.summaryTileTitle("Violating Components")).isVisible();
    assertThat(page.summaryTileValue("Violating Components")).containsText("1");
    assertThat(page.summaryTileTitle("Active Waivers")).isVisible();
    assertThat(page.summaryTileValue("Active Waivers")).containsText("0");
  }

  public void shouldShowImpactTableColumns() {
    assertThat(page.tableHeadSection().locator("th")).hasText(
        new String[]{"Application", "Stage", "Component", "Version", "CVE ID",
          "Recommended Action", "Active Waiver", "Violating", "Evaluation", "Evaluation Date"});
  }

  public void shouldShowTableRowWithData(String appName, String cveId) {
    assertThat(page.impactTable().getByText(appName)).isVisible();
    assertThat(page.impactTable().getByText(cveId)).isVisible();
  }

  public void shouldShowEmptyTable() {
    assertThat(page.impactTable()).isVisible();
    assertThat(page.tableEmptyMessage()).isVisible();
  }
}
