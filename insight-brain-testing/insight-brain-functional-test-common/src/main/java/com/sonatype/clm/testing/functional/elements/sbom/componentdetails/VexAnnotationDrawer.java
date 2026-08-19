/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class VexAnnotationDrawer
    extends BasicElement<VexAnnotationDrawer>
{
  static final String ROOT_SELECTOR = "#vex-annotation-popover";

  public VexAnnotationDrawer() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child("header .nx-h2");
  }

  public SelenideElement packageUrl() {
    return child(".vex-annotation-drawer-header-popover__package-url");
  }

  public SelenideElement cvssScore() {
    return child("span[data-testid='cvssScore']");
  }

  public SelenideElement verificationStatus() {
    return child("span[data-testid='verification-text']");
  }

  public SelenideElement vulnerabilityDescription() {
    return child(".vulnerability-description-paragraph");
  }

  public SelenideElement analysisStatusDropdown() {
    return child("#vex-annotation-drawer__form__analysis-status-select");
  }

  public SelenideElement justificationDropdown() {
    return child("#vex-annotation-drawer__form__justification-select");
  }

  public SelenideElement responseDropdown() {
    return child("#vex-annotation-drawer__form__response-select");
  }

  public SelenideElement submitButton() {
    return child(".vex-annotation-popover__footer-button-bar .vex-annotation-drawer__form__submit-button");
  }

  public SelenideElement closeButton() {
    return child("header .nx-btn--close");
  }

  public SelenideElement formFooterAlert() {
    return child(".vex-annotation-popover__footer-nx-drawer .nx-alert__content");
  }

  public SelenideElement successModal() {
    return child(".nx-submit-mask__message");
  }

  public SelenideElement annotationDetails() {
    return child("textarea");
  }

  public SelenideElement unsavedChangesModal() {
    return $("#unsaved-modal");
  }

  public SelenideElement unsavedChangesModalHeader() {
    return $("#unsaved-modal .nx-h2");
  }

  public SelenideElement unsavedChangesModalBody() {
    return $("#unsaved-modal .nx-alert__content");
  }

  public SelenideElement unsavedChangesModalCancelButton() {
    return $("#unsaved-modal .nx-btn-bar .nx-btn:first-child");
  }

  public SelenideElement unsavedChangesModalContinueButton() {
    return $("#unsaved-modal .nx-btn--primary");
  }
}
