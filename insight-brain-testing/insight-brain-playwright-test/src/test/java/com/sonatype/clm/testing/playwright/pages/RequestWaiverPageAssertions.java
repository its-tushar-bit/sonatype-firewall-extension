/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RequestWaiverPageAssertions
{
  private final RequestWaiverPage page;

  public RequestWaiverPageAssertions(RequestWaiverPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowPageLayout(
      String expectedTitle,
      String expectedComponent,
      String expectedPolicy,
      String expectedConstraint,
      String expectedCondition,
      int expectedScopeOptionCount,
      int expectedComponentRadioCount,
      int expectedExpiryOptionCount,
      int expectedWaiverReasonOptionCount)
  {
    assertThat(page.title()).containsText(expectedTitle);
    assertThat(page.componentSection()).containsText(expectedComponent);
    assertThat(page.policySection()).containsText(expectedPolicy);
    assertThat(page.constraintSection()).containsText(expectedConstraint);
    assertThat(page.conditionsSection()).containsText(expectedCondition);
    assertThat(page.scopeOptions()).hasCount(expectedScopeOptionCount);
    assertThat(page.componentRadios()).hasCount(expectedComponentRadioCount);
    assertThat(page.comments()).isEmpty();
    page.assertSharedFormShellLayout(expectedExpiryOptionCount, expectedWaiverReasonOptionCount);
    assertThat(page.submitButton()).isVisible();
  }

  public void shouldHaveNoSubmitError() {
    assertThat(page.submitError()).isHidden();
  }

  public void shouldShowSubmitError(String expectedMessage) {
    assertThat(page.submitError()).isVisible();
    assertThat(page.submitError()).containsText(expectedMessage);
  }

  public void shouldBeHidden() {
    assertThat(page.container()).isHidden();
  }

  public void shouldShowEnterprisePreviewMode(String alertText, String goBackLinkText) {
    assertThat(page.enterprisePreviewAlert()).isVisible();
    assertThat(page.enterprisePreviewAlert()).containsText(alertText);
    assertThat(page.enterpriseGoBackLink()).containsText(goBackLinkText);
  }

  public void shouldShowNoteToReviewerField(String label, String sublabel, String maxLength) {
    assertThat(page.noteToReviewerLabel()).containsText(label);
    assertThat(page.noteToReviewerSublabel()).containsText(sublabel);
    assertThat(page.noteToReviewer()).hasAttribute("maxlength", maxLength);
  }
}
