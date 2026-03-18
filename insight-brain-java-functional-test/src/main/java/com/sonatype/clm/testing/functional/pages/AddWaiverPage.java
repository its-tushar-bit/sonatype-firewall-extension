/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.IqVulnerabilityModal;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AddWaiverPage
    extends BasicElement<AddWaiverPage>
{
  public static final String ROOT = "#add-waiver-page";

  public static String url(String violationId) {
    return url(violationId, null);
  }

  public static String url(String violationId, String comments) {
    return url(violationId, comments, null);
  }

  public static String url(String violationId, String comments, String reasonId) {
    if (comments != null && reasonId != null) {
      return BaseUrl.resolvePageUrl("/addWaiver/{id}?comments={comments}&reasonId={reasonId}",
          violationId, comments, reasonId);
    }
    else if (comments != null) {
      return BaseUrl.resolvePageUrl("/addWaiver/{id}?comments={comments}", violationId, comments);
    }
    else if (reasonId != null) {
      return BaseUrl.resolvePageUrl("/addWaiver/{id}?reasonId={reasonId}", violationId, reasonId);
    }
    else {
      return BaseUrl.resolvePageUrl("/addWaiver/{id}", violationId);
    }
  }

  public AddWaiverPage() {
    super(ROOT);
  }

  public SelenideElement artifactName() {
    return child(".iq-add-waiver-form__component .nx-read-only__label span");
  }

  public SelenideElement componentName() {
    return child(".iq-add-waiver-form__component .nx-read-only__data");
  }

  public SelenideElement policyName() {
    return child(".iq-add-waiver-form__policy .iq-threat-level");
  }

  public SelenideElement constraintName() {
    return child(".iq-add-waiver-form__constraint .nx-read-only__data");
  }

  public SelenideElement currentUserName() {
    return child(".iq-add-waiver-form__created-by .nx-read-only__data");
  }

  public ElementsCollection conditions() {
    return children(".iq-add-waiver-form__conditions .nx-read-only__data span");
  }

  public SelenideElement condition(int index) {
    return child(".iq-add-waiver-form__conditions .nx-read-only__data span", nthChild(index));
  }

  public SelenideElement vulnerabilityDetailsLink() {
    return child(".iq-add-waiver-form__vulnerability_details_link a");
  }

  public IqVulnerabilityModal vulnerabilityModal() {
    return new IqVulnerabilityModal("#vulnerability-details-modal");
  }

  public NxFormSelect availableScopesDropdown() {
    return new NxFormSelect("#iq-add-waiver-scope");
  }

  public ElementsCollection availableScopes() {
    NxFormSelect dropdown = availableScopesDropdown();
    return dropdown.listItems();
  }

  public SelenideElement scope(int index) {
    NxFormSelect dropdown = availableScopesDropdown();
    return dropdown.listItem(index);
  }

  public ElementsCollection availableComponents() {
    return children(".iq-add-waiver-form__components .nx-radio");
  }

  public NxRadio component(int index) {
    return new NxRadio(this.availableComponents().get(index));
  }

  public SelenideElement expiryTimesSelect() {
    return child("#waiver-expiration-select");
  }

  public ElementsCollection expiryTimesOptions() {
    return children("#waiver-expiration-select option");
  }

  public SelenideElement waiverReasonSelect() {
    return child("#waiver-reason-select");
  }

  public ElementsCollection waiverReasonOptions() {
    return children("#waiver-reason-select option");
  }

  public SelenideElement customExpiryTime() {
    return child(".iq-add-waiver-form__date-input .nx-text-input__input");
  }

  public SelenideElement customExpiryTimeErrorMessage() {
    return child(".iq-add-waiver-form__date-input .nx-field-validation-message");
  }

  public SelenideElement expiryTimeMessage() {
    return child(".iq-add-waiver-form__expiration-days-diff");
  }

  public SelenideElement comments() {
    return child(".iq-add-waiver-form__comments .nx-text-input textarea");
  }

  public Button saveButton() {
    return new Button(".add-waiver-submit");
  }

  public Button cancelButton() {
    return new Button(".nx-form__cancel-btn");
  }

  public SelenideElement submitError() {
    return child(".nx-footer .nx-alert");
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }
}
