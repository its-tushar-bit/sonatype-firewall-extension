/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link AdministratorsPage}.
 */
public class AdministratorsPageAssertions
{
  private final AdministratorsPage page;

  public AdministratorsPageAssertions(AdministratorsPage page) {
    this.page = page;
  }

  public void shouldHaveRowCount(int expected) {
    assertThat(page.rows()).hasCount(expected);
  }

  public void rowShouldHaveRole(int rowIndex, String expectedRole) {
    assertThat(page.roleCell(rowIndex)).hasText(expectedRole);
  }

  public void rowShouldHaveMembers(int rowIndex, String expectedMembers) {
    assertThat(page.membersCell(rowIndex)).containsText(expectedMembers);
  }
}
