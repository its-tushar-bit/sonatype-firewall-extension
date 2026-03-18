/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class RequestWaiverPage
    extends BasicElement<RequestWaiverPage>
{
  private static final String ROOT_SELECTOR = "#request-waiver-page";

  public static String url(String violationId) {
    return BaseUrl.resolvePageUrl("/requestWaiver/{id}", violationId);
  }

  public static String urlWithQueryParams(String violationId, String type, String sidebarReference) {
    return BaseUrl.resolvePageUrl(
        "/requestWaiver/{id}?type={type}&sidebarReference={sidebarReference}",
        violationId,
        type,
        sidebarReference);
  }

  public SelenideElement root() {
    return $(ROOT_SELECTOR);
  }

  public SelenideElement requestWaiverPolicyViolationId() {
    return child("#request-waivers-policy-violation-id");
  }

  public SelenideElement requestWaiverHeader() {
    return child(".nx-h1");
  }

  public SelenideElement requestWaiverTitle() {
    return child(".nx-h2");
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
    return children(".iq-request-waiver-form__components .nx-radio__input");
  }

  public ElementsCollection requestWaiverComponentsOptions() {
    return children(".iq-request-waiver-form__components .nx-radio__content");
  }

  public SelenideElement requestWaiverExpiryTime() {
    return child(".iq-request-waiver-form__expiryTime");
  }

  public ElementsCollection requestWaiverExpiryTimeOptions() {
    return children("#waiver-expiration-select option");
  }

  public SelenideElement waiverExpirationSelect() {
    return child("#waiver-expiration-select");
  }

  public SelenideElement requestWaiverReason() {
    return child(".iq-request-waiver-form__reason");
  }

  public ElementsCollection requestWaiverReasonOptions() {
    return children("#waiver-reason-select option");
  }

  public SelenideElement waiverReasonSelect() {
    return child("#waiver-reason-select");
  }

  public SelenideElement requestWaiverComments() {
    return child(".iq-request-waiver-form__comments .nx-text-input textarea");
  }

  public SelenideElement requestWaiverNoteToReviewer() {
    return child(".iq-request-waiver-form__note-to-reviewer .nx-text-input textarea");
  }

  public SelenideElement requestWaiverDateInput() {
    return child(".iq-request-waiver-form__date-input .nx-text-input__input");
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }

  public Button saveButton() {
    return new Button(".request-waiver-submit");
  }

  public Button cancelButton() {
    return new Button(".nx-form__cancel-btn");
  }

  public SelenideElement submitError() {
    return child(".nx-footer .nx-alert");
  }

  public SelenideElement rejectionErrorAlert() {
    return child(".nx-alert--error");
  }
}
