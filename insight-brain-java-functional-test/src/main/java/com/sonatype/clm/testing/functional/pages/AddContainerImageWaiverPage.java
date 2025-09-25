/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class AddContainerImageWaiverPage
    extends BasicElement<AddContainerImageWaiverPage>
{
  public static final String ROOT = "#add-firewall-container-image-waiver-page";

  public AddContainerImageWaiverPage() {
    super(ROOT);
  }

  public static String url(String containerImageId, String scanId) {
    return BaseUrl.resolvePageUrl(
        "/firewall/containerReport/{publicId}/{scanId}/policy/addContainerImageWaiver",
        containerImageId,
        scanId);
  }

  public SelenideElement pageTitle() {
    return child(".nx-h1");
  }

  public SelenideElement waiverFormHeader() {
    return child("#container-waiver-config-header");
  }

  public SelenideElement expiryTimesSelect() {
    return child("#add-container-image-waiver-expiration-select");
  }

  public ElementsCollection expiryTimesOptions() {
    return children("#add-container-image-waiver-expiration-select option");
  }

  public SelenideElement customExpiryTime() {
    return child(".iq-add-waiver-form__date-input .nx-text-input__input");
  }

  public SelenideElement customExpiryTimeValidationMessage() {
    return child(".iq-add-waiver-form__date-input .nx-field-validation-message");
  }

  public SelenideElement daysDiffMessage() {
    return child(".add-waiver-expiration-days-diff");
  }

  public SelenideElement waiverReasonSelect() {
    return child("#add-container-image-waiver-reason-select");
  }

  public ElementsCollection waiverReasonOptions() {
    return children("#add-container-image-waiver-reason-select option");
  }

  public SelenideElement comments() {
    return child("#add-container-image-comments");
  }

  public SelenideElement submit() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child(".nx-form__cancel-btn");
  }

  public SelenideElement threatCounter(String category) {
    return child(".nx-small-threat-counter--" + category);
  }

  public SelenideElement violationText() {
    return child(".iq-caption .iq-caption__text");
  }

  public SelenideElement violationSubText() {
    return child(".iq-caption .iq-caption__sub-text");
  }

  public SelenideElement infoAlert() {
    return child(".nx-alert--info");
  }

  public SelenideElement policyLabel() {
    return child(".add-waiver-policy .nx-read-only__label");
  }

  public SelenideElement policyValue() {
    return child(".add-waiver-policy .nx-read-only__data");
  }

  public SelenideElement containerImageLabel() {
    return child(".add-waiver-container-image .nx-read-only__label");
  }

  public SelenideElement containerImageValue() {
    return child(".add-waiver-container-image .nx-read-only__data");
  }

  public SelenideElement submitError() {
    return child(".nx-footer .nx-alert");
  }
}
