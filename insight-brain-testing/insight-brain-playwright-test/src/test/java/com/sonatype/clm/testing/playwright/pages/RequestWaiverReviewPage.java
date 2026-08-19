/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;

public class RequestWaiverReviewPage
    extends BasePage
{
  private static final String ROOT = "#request-waiver-review-page";

  private static final String READY_MARKER = ROOT + " .iq-request-waiver-info";

  private static final Locator.GetByRoleOptions APPROVE_OPTS =
      new Locator.GetByRoleOptions().setName("Approve");

  private static final Locator.GetByRoleOptions REJECT_OPTS =
      new Locator.GetByRoleOptions().setName("Reject Waiver Request");

  private static final Locator.GetByRoleOptions SEND_OPTS =
      new Locator.GetByRoleOptions().setName("Send");

  public RequestWaiverReviewPage() {
    super();
  }

  public static String url(String ownerType, String ownerId, String policyWaiverRequestId) {
    return "/assets/index.html#/requestWaiverReview/" + ownerType + "/" + ownerId + "/" + policyWaiverRequestId;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator title() {
    return container().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Review Requested Waiver"));
  }

  public Locator requesterInfo() {
    return locator(ROOT + " .iq-request-waiver-info");
  }

  public Locator requestedByValue() {
    return container().getByText("Requested By").locator("..").locator(".nx-read-only__data");
  }

  public Locator dateRequestedValue() {
    return container().getByText("Date Requested").locator("..").locator(".nx-read-only__data");
  }

  public Locator noteToReviewerBlockquote() {
    return container().getByRole(AriaRole.BLOCKQUOTE);
  }

  public Locator componentSection() {
    return locator(ROOT + " .iq-request-waiver-form__component");
  }

  public Locator policySection() {
    return container().getByText("Policy", new Locator.GetByTextOptions().setExact(true)).locator("..");
  }

  public Locator constraintSection() {
    return container().getByText("Constraint Name").locator("..");
  }

  public Locator conditionsSection() {
    return container().getByText("Conditions").locator("..");
  }

  public Locator scopeSelect() {
    return container().getByLabel("select scope");
  }

  public Locator scopeOptions() {
    return scopeSelect().getByRole(AriaRole.OPTION);
  }

  public Locator componentRadios() {
    return container().getByRole(AriaRole.RADIO);
  }

  public Locator allVersionsRadio() {
    return container().getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("all versions"));
  }

  public Locator allVersionsTooltip() {
    return container().getByRole(AriaRole.TOOLTIP);
  }

  public Locator expiryTimeSelect() {
    return container().getByLabel("select waiver expiration");
  }

  public Locator expiryTimeOptions() {
    return expiryTimeSelect().getByRole(AriaRole.OPTION);
  }

  public Locator waiverReasonSelect() {
    return container().getByLabel("Reason");
  }

  public Locator comments() {
    return container().getByRole(AriaRole.TEXTBOX).last();
  }

  public Locator approveButton() {
    return container().getByRole(AriaRole.BUTTON, APPROVE_OPTS);
  }

  public Locator rejectButton() {
    return container().getByRole(AriaRole.BUTTON, REJECT_OPTS);
  }

  public Locator rejectionModal() {
    return container().getByRole(AriaRole.DIALOG);
  }

  public Locator rejectionModalTitle() {
    return rejectionModal().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Reject Waiver Request"));
  }

  public Locator rejectionReasonTextarea() {
    return rejectionModal().getByRole(AriaRole.TEXTBOX);
  }

  public Locator rejectionSendButton() {
    return rejectionModal().getByRole(AriaRole.BUTTON, SEND_OPTS);
  }

  public Locator rejectionModalCancelButton() {
    return rejectionModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator rejectionModalError() {
    return rejectionModal().getByRole(AriaRole.ALERT);
  }

  public Locator rejectionModalValidationError() {
    return rejectionModal().locator(".nx-text-input--invalid");
  }

  public Locator errorAlert() {
    return container().getByRole(AriaRole.ALERT);
  }

  public Locator submitError() {
    return container().getByRole(AriaRole.CONTENTINFO).getByRole(AriaRole.ALERT);
  }

  public void clickApprove() {
    approveButton().click();
  }

  public void clickReject() {
    rejectButton().click();
  }

  public void fillRejectionReason(String reason) {
    rejectionReasonTextarea().fill(reason);
  }

  public void clickSendRejection() {
    rejectionSendButton().click();
  }

  public void clickRejectionModalCancel() {
    rejectionModalCancelButton().click();
  }

  public void selectExpiryTime(String value) {
    expiryTimeSelect().selectOption(value);
  }

  public void waitUntilLoaded() {
    if (!page.url().contains("requestWaiverReview")) {
      PlaywrightWaitUtils.waitForUrl(page, "requestWaiverReview");
    }
    waitForSpinnersHidden(ROOT + " .nx-loading-spinner");
    locator(READY_MARKER)
        .waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS));
  }

  public void waitUntilScopeReady() {
    waitUntilLoaded();
    PlaywrightWaitUtils.waitForCondition(
        () -> {
          Locator options = scopeSelect().getByRole(AriaRole.OPTION);
          return options.count() > 0
              && options.first().textContent() != null
              && !options.first().textContent().isBlank();
        },
        PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS,
        "Timed out waiting for waiver scope options to load");
  }

  private void waitForSpinnersHidden(String spinnerSelector) {
    Locator spinner = locator(spinnerSelector).first();
    try {
      spinner.waitFor(new Locator.WaitForOptions()
          .setState(WaitForSelectorState.HIDDEN)
          .setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
    }
    catch (TimeoutError ignored) {
      // Spinner may never appear on fast loads.
    }
  }

}
