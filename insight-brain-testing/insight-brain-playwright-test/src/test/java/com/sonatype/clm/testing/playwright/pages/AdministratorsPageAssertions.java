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

  public void shouldShowContainer() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowPageTitle() {
    assertThat(page.pageTitle()).isVisible();
  }

  public void shouldShowTileHeader() {
    assertThat(page.tileHeader()).isVisible();
  }

  public void shouldShowTableHeaderRoleColumn(String expected) {
    assertThat(page.tableHeaderRoleCell()).hasText(expected);
  }

  public void shouldShowTableHeaderMembersColumn(String expected) {
    assertThat(page.tableHeaderMembersCell()).hasText(expected);
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

  public void shouldShowChevron(int rowIndex) {
    assertThat(page.chevron(rowIndex)).isVisible();
  }

  public void shouldShowErrorWithRetryButton() {
    assertThat(page.errorMessage()).isVisible();
    assertThat(page.retryButton()).isVisible();
  }

  public void shouldShowEmptyMessage(String expected) {
    assertThat(page.emptyMessage()).isVisible();
    assertThat(page.emptyMessage()).containsText(expected);
  }
}
