/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.List;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link DashboardFiltersComponent}.
 */
public class DashboardFiltersComponentAssertions
{
  private final DashboardFiltersComponent page;

  public DashboardFiltersComponentAssertions(DashboardFiltersComponent page) {
    this.page = page;
  }

  public void shouldShowWaiverReasonOptions(List<String> expectedOptions) {
    Pattern edgeWhitespace =
        Pattern.compile("^[\\s\\u00A0]+|[\\s\\u00A0]+$");

    List<String> labels = page.policyWaiverReasonFilter()
        .locator("label")
        .allTextContents()
        .stream()
        .map(s -> edgeWhitespace.matcher(s).replaceAll(""))
        .toList();

    Assertions.assertThat(labels).containsExactlyElementsOf(expectedOptions);
  }

  public void shouldBeVisible() {
    assertThat(page.filterContainer()).isVisible();
  }

  public void shouldBeHidden() {
    assertThat(page.filterContainer()).isHidden();
  }
}
