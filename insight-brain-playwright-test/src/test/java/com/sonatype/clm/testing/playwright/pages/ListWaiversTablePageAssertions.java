/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ListWaiversTablePageAssertions
{
  private final ListWaiversTablePage page;

  public ListWaiversTablePageAssertions(ListWaiversTablePage page) {
    this.page = page;
  }

  public void shouldShowAutoWaiverWithoutDeleteButton() {
    assertThat(page.autoWaiverRow()).isVisible();
    assertThat(page.autoWaiverRow().locator(".list-waivers-row__delete-btn")).hasCount(0);
  }

  public void shouldShowAutoWaiverBeforeActiveWaivers() {
    Locator firstRow = page.allRows().first();
    assertThat(firstRow).hasClass(java.util.regex.Pattern.compile(".*list-auto-waiver-row.*"));
  }

  public void shouldShowAutoWaiverTag(String tagText) {
    assertThat(page.autoWaiverTag()).containsText(tagText);
  }

  public void shouldShowActiveWaiverCount(int count) {
    assertThat(page.activeWaiverRows()).hasCount(count);
  }

  public void shouldShowExpiredWaiverCount(int count) {
    assertThat(page.expiredWaiverRows()).hasCount(count);
  }

  public void shouldShowExpiredWaiversAfterActiveWaivers() {
    Locator rows = page.allRows();
    int totalCount = rows.count();
    boolean foundExpired = false;
    for (int i = 0; i < totalCount; i++) {
      String classes = rows.nth(i).getAttribute("class");
      if (classes != null && classes.contains("list-waivers-row--expired")) {
        foundExpired = true;
      }
      else if (foundExpired && (classes == null || !classes.contains("list-auto-waiver-row"))) {
        throw new AssertionError("Found non-expired row after expired row at index " + i);
      }
    }
  }

  public void shouldShowEmptyState(String text, String linkText, String linkHref) {
    assertThat(page.emptyMessage()).isVisible();
    assertThat(page.emptyMessage()).containsText(text);
    assertThat(page.emptyMessageLink()).isVisible();
    assertThat(page.emptyMessageLink()).hasAttribute("href", linkHref);
    assertThat(page.emptyMessageLink()).containsText(linkText);
  }

  public void shouldShowLoadingState(int metaRowCount) {
    assertThat(page.loadingSpinner()).isVisible();
    assertThat(page.allRows()).hasCount(metaRowCount);
  }

  public void shouldShowErrorWithRetry() {
    assertThat(page.errorMessage()).isVisible();
    assertThat(page.retryButton()).isVisible();
  }

  public void shouldShowDeleteModal(String heading) {
    assertThat(page.deleteWaiverModal()).isVisible();
    assertThat(page.deleteWaiverModalHeading()).containsText(heading);
  }

  public void shouldNotShowDeleteModal() {
    assertThat(page.deleteWaiverModal()).not().isVisible();
  }
}
