/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.time.Duration;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class RequestWaiverReviewPage
    extends BasicElement<RequestWaiverReviewPage>
{
  private static final String ROOT_SELECTOR = "#request-waiver-review-page";

  public static String url(String ownerType, String ownerId, String policyWaiverRequestId) {
    return BaseUrl.resolvePageUrl(
        "/requestWaiverReview/{ownerType}/{ownerId}/{policyWaiverRequestId}",
        ownerType,
        ownerId,
        policyWaiverRequestId);
  }

  public SelenideElement root() {
    return $(ROOT_SELECTOR);
  }

  public static SelenideElement pageLoadSpinner() {
    return $(".nx-loading-spinner");
  }

  public static void waitUntilSpinnersGone() {
    pageLoadSpinner().shouldNotBe(visible, Duration.ofSeconds(10));
  }

  public SelenideElement requestWaiverReviewAlert() {
    return child(".nx-alert");
  }

  public SelenideElement requestWaiverReviewHeader() {
    return child(".nx-h1");
  }

  public SelenideElement requestWaiverReviewInfoTitle() {
    return child(".nx-tile-header .nx-tile-header__title .nx-h2");
  }

  public SelenideElement requestWaiverInfoRequestedBy() {
    return child(".iq-request-waiver-info__requested-by");
  }

  public SelenideElement requestWaiverInfoDateRequested() {
    return child(".iq-request-waiver-info__date-requested");
  }

  public SelenideElement requestWaiverInfoNoteToReviewer() {
    return child(".iq-request-waiver-info__note-to-reviewer");
  }

  public SelenideElement waiverConfigurationTitle() {
    return child(".iq-request-waiver-form .nx-tile-header .nx-tile-header__title .nx-h2");
  }

  public SelenideElement requestWaiverComponentName() {
    return child(".iq-request-waiver-form__component");
  }

  public SelenideElement requestWaiverPolicy() {
    return child(".iq-request-waiver-form__policy");
  }

  public SelenideElement requestWaiverConstraint() {
    return child(".iq-request-waiver-form__constraint");
  }

  public SelenideElement requestWaiverConditions() {
    return child(".iq-request-waiver-form__conditions");
  }

  public SelenideElement requestWaiverScope() {
    return child(".iq-request-waiver-form__scope");
  }

  public ElementsCollection requestWaiverScopeOptions() {
    return children("#iq-request-waiver-scope option");
  }

  public SelenideElement requestWaiverScopeSelect() {
    return child("#iq-request-waiver-scope");
  }

  public SelenideElement requestWaiverComponents() {
    return child(".iq-request-waiver-form__components");
  }

  public ElementsCollection requestWaiverComponentsRadios() {
    return children(".iq-request-waiver-form__components .nx-radio");
  }

  public ElementsCollection requestWaiverComponentsInputs() {
    return children(".iq-request-waiver-form__components .nx-radio__input");
  }

  public ElementsCollection requestWaiverComponentsOptions() {
    return children(".iq-request-waiver-form__components .nx-radio__content");
  }

  public NxRadio requestWaiverComponent(int index) {
    return new NxRadio(this.requestWaiverComponentsRadios().get(index));
  }

  public SelenideElement requestWaiverExpiryTime() {
    return child(".iq-request-waiver-form__expiryTime");
  }

  public ElementsCollection requestWaiverExpiryTimeOptions() {
    return children("#waiver-expiration-select option");
  }

  public SelenideElement requestWaiverExpiryTimesSelect() {
    return child("#waiver-expiration-select");
  }

  public SelenideElement requestWaiverReason() {
    return child(".iq-request-waiver-form__reason");
  }

  public ElementsCollection requestWaiverReasonOptions() {
    return children("#waiver-reason-select option");
  }

  public SelenideElement requestWaiverReasonSelect() {
    return child("#waiver-reason-select");
  }

  public SelenideElement requestWaiverComments() {
    return child(".iq-request-waiver-form__comments .nx-text-input textarea");
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }

  public Button approveButton() {
    return new Button(".request-waiver-approve-btn");
  }

  public Button cancelButton() {
    return new Button(".nx-form__cancel-btn");
  }

  public SelenideElement submitError() {
    return child(".nx-footer .nx-alert");
  }

  public Button rejectButton() {
    return new Button(".request-waiver-reject-btn");
  }

  public SelenideElement requestWaiverRejectTitle() {
    return child(".iq-request-waiver-modal .nx-modal-header .nx-h2");
  }

  public SelenideElement requestWaiverRejectLegend() {
    return child(".iq-request-waiver-modal .nx-legend__text");
  }

  public SelenideElement requestWaiverRejectReason() {
    return child(".iq-request-waiver-modal .nx-text-input__input");
  }

  public Button sendRejectionButton() {
    return new Button(".iq-request-waiver-modal .request-waiver-send-rejection");
  }

  public Button cancelRejectionButton() {
    return new Button(".iq-request-waiver-modal .nx-form__cancel-btn");
  }

  public SelenideElement rejectionErrorAlert() {
    return child(".nx-alert--error");
  }
}
