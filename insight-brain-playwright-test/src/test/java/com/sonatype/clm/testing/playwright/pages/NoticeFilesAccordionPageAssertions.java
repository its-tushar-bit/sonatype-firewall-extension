/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link NoticeFilesAccordionPage}.
 */
public class NoticeFilesAccordionPageAssertions
{
  private final NoticeFilesAccordionPage page;

  public NoticeFilesAccordionPageAssertions(NoticeFilesAccordionPage page) {
    this.page = page;
  }

  public void shouldShowTile() {
    assertThat(page.tile()).isVisible();
  }

  public void shouldShowEditButton() {
    assertThat(page.editNoticesButton()).isVisible();
  }

  public void shouldShowNoneFound() {
    assertThat(page.noneFoundText()).isVisible();
  }

  public void shouldShowModal() {
    assertThat(page.modal()).isVisible();
  }

  public void shouldHideModal() {
    assertThat(page.modal()).isHidden();
  }

  public void shouldHaveNoticeRowCount(int expectedCount) {
    assertThat(page.noticeRows()).hasCount(expectedCount);
  }

  public void shouldShowAddNoticeButton() {
    assertThat(page.addNoticeButton()).isVisible();
  }

  public void shouldShowAddIcon() {
    assertThat(page.editButtonIcon()).hasAttribute("data-icon", "plus");
  }

  public void shouldShowEditIcon() {
    assertThat(page.editButtonIcon()).hasAttribute("data-icon", "pen");
  }
}
