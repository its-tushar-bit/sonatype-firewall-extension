/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link RequestWaiverUpdatePage}.
 */
public class RequestWaiverUpdatePageAssertions
{
  private final RequestWaiverUpdatePage page;

  public RequestWaiverUpdatePageAssertions(RequestWaiverUpdatePage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowUpdateLayout(String expectedTitle, String expectedComponent, String expectedPolicy) {
    assertThat(page.title()).containsText(expectedTitle);
    assertThat(page.componentSection()).containsText(expectedComponent);
    assertThat(page.policySection()).containsText(expectedPolicy);
  }

  public void shouldShowRejectionAlert(String expectedAlertText, String expectedRejectionReason) {
    assertThat(page.errorAlert()).isVisible();
    assertThat(page.errorAlert()).containsText(expectedAlertText);
    assertThat(page.errorAlert()).containsText(expectedRejectionReason);
  }

  public void shouldShowSavedCommentAndNote(String expectedComment, String expectedNoteToReviewer) {
    assertThat(page.comments()).hasValue(expectedComment);
    assertThat(page.noteToReviewer()).hasValue(expectedNoteToReviewer);
  }
}
