/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertions companion for {@link DashboardApplicationsComponent}.
 */
public class DashboardApplicationsComponentAssertions
{
  private final DashboardApplicationsComponent page;

  public DashboardApplicationsComponentAssertions(DashboardApplicationsComponent page) {
    this.page = page;
  }

  public void shouldHaveApplicationOrder(List<String> orderedNameSubstrings) {
    for (int i = 0; i < orderedNameSubstrings.size(); i++) {
      assertThat(page.applications().nth(i)).containsText(orderedNameSubstrings.get(i));
    }
  }
}
