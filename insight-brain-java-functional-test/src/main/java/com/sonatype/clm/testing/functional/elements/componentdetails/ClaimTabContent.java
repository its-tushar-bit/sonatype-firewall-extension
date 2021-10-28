/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;

import com.codeborne.selenide.SelenideElement;

public class ClaimTabContent
    extends BasicElement<ClaimTabContent>
{
  public static final String CLAIM_TAB_SELECTOR = "#component-details-claim-unknown-component";

  public ClaimTabContent() {
    super(CLAIM_TAB_SELECTOR);
  }

  public SelenideElement title() {
    return child(".nx-h2");
  }

  public SelenideElement groupId() {
    return child("#groupId");
  }

  public SelenideElement extension() {
    return child("#extension");
  }

  public SelenideElement artifactId() {
    return child("#artifactId");
  }

  public SelenideElement createdTime() {
    return child("#created");
  }

  public SelenideElement version() {
    return child("#version");
  }

  public SelenideElement classifier() {
    return child("#classifier");
  }

  public SelenideElement comment() {
    return child("#comment");
  }

  public SelenideElement cancel() {
    return child("#component-details-claim-cancel");
  }

  public SelenideElement revoke() {
    return child("#component-details-claim-revoke");
  }

  public SelenideElement claim() {
    return child(".nx-form__submit-btn");
  }

  public static SelenideElement getInputValidationElement(SelenideElement element) {
    return element.closest(".nx-form-group").find(".nx-text-input__invalid-message");
  }

  public List<SelenideElement> requiredFields() {
    return Arrays.asList(groupId(), extension(), artifactId(), version());
  }

  public List<SelenideElement> allTextFields() {
    return Arrays.asList(groupId(), extension(), artifactId(), version(), classifier(), comment());
  }

  public NxDeleteModal getDeleteModal() {
    return new NxDeleteModal("#component-details-revoke-claim-modal");
  }
}
