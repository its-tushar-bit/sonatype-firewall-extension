/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RequestWaiverReviewPageAssertions
{
  private final RequestWaiverReviewPage page;

  public RequestWaiverReviewPageAssertions(RequestWaiverReviewPage page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    page.waitUntilLoaded();
    assertThat(page.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  public void shouldShowPageTitle(String expectedTitle) {
    assertThat(page.title()).containsText(expectedTitle);
  }

  public void shouldShowRequesterInfo(String expectedRequesterName, String expectedNoteToReviewer) {
    assertThat(page.requestedByValue()).containsText(expectedRequesterName);
    assertThat(page.dateRequestedValue()).not().isEmpty();
    assertThat(page.noteToReviewerBlockquote()).containsText(expectedNoteToReviewer);
  }

  public void shouldShowWaiverConfiguration(
      String expectedComponent,
      String expectedPolicy,
      String expectedConstraint,
      String expectedCondition)
  {
    assertThat(page.componentSection()).containsText(expectedComponent);
    assertThat(page.policySection()).containsText(expectedPolicy);
    assertThat(page.constraintSection()).containsText(expectedConstraint);
    assertThat(page.conditionsSection()).containsText(expectedCondition);
  }

  public void shouldShowApproveAndRejectButtons() {
    assertThat(page.approveButton()).isVisible();
    assertThat(page.rejectButton()).isVisible();
  }

  public void shouldShowRejectionModal(String expectedTitle) {
    assertThat(page.rejectionModal()).isVisible();
    assertThat(page.rejectionModalTitle()).containsText(expectedTitle);
    assertThat(page.rejectionReasonTextarea()).isVisible();
    assertThat(page.rejectionSendButton()).isVisible();
  }

  public void shouldShowRejectedStatusAlert(String expectedAlertText) {
    assertThat(page.errorAlert()).isVisible();
    assertThat(page.errorAlert()).containsText(expectedAlertText);
  }

  public void shouldShowReadOnlyState() {
    assertThat(page.approveButton()).isDisabled();
    assertThat(page.rejectButton()).isDisabled();
    assertThat(page.scopeSelect()).isDisabled();
    assertThat(page.expiryTimeSelect()).isDisabled();
    assertThat(page.waiverReasonSelect()).isDisabled();
  }

  public void shouldDisableApproveAndRejectButtons() {
    assertThat(page.approveButton()).isDisabled();
    assertThat(page.rejectButton()).isDisabled();
  }

  public void shouldShowAllVersionsDisabledWithTooltip(String expectedTooltipText) {
    assertThat(page.allVersionsRadio()).isDisabled();
    assertThat(page.allVersionsTooltip()).containsText(expectedTooltipText);
  }

  public void shouldShowScopeOptionText(String expectedText) {
    page.waitUntilScopeReady();
    assertThat(page.scopeSelect()).containsText(expectedText);
  }

  public void shouldShowRejectionModalValidationState() {
    assertThat(page.rejectionSendButton()).isVisible();
    assertThat(page.rejectionReasonTextarea()).isVisible();
  }

  public void shouldShowRejectionModalErrorHidden() {
    assertThat(page.rejectionModalError()).not().isVisible();
  }

  public void shouldShowRejectionModalDismissed() {
    assertThat(page.rejectionModal()).not().isVisible();
  }

  public void shouldShowFormFieldsDisabled() {
    assertThat(page.scopeSelect()).isDisabled();
    assertThat(page.expiryTimeSelect()).isDisabled();
    assertThat(page.waiverReasonSelect()).isDisabled();
    assertThat(page.comments()).isDisabled();
  }

  public void shouldShowRejectionTextareaPlaceholder(String expectedPlaceholder) {
    assertThat(page.rejectionReasonTextarea()).hasAttribute("placeholder", expectedPlaceholder);
  }

  public void shouldShowRejectionTextareaMaxLength(String expectedMaxLength) {
    assertThat(page.rejectionReasonTextarea()).hasAttribute("maxlength", expectedMaxLength);
  }

  public void shouldShowSendButtonLabel(String expectedLabel) {
    assertThat(page.rejectionSendButton()).containsText(expectedLabel);
  }
}
