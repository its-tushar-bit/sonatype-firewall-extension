/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;

import com.codeborne.selenide.SelenideElement;

public class WaiverConfirmationPage
    extends BasicElement<WaiverConfirmationPage>
{
  public static final String ROOT = ".iq-bulk-waiver-confirmation-page";

  public WaiverConfirmationPage() {
    super(ROOT);
  }

  public BulkWaiveTitle title() {
    return new BulkWaiveTitle();
  }

  public SelenideElement tileHeaderTitle() {
    return child(".nx-tile-header__title h2");
  }

  // Fieldsets appear in order: Violations, Policy Violations, Scope, Components, Expiration, Reason, Comment
  public SelenideElement violationsBeingWaivedValue() {
    return child(".nx-tile-content .nx-fieldset:nth-of-type(1) .nx-read-only__data");
  }

  public SelenideElement policyViolationsThreatCounter() {
    return child(".nx-tile-content .nx-fieldset:nth-of-type(2) .nx-threat-counter");
  }

  public SelenideElement scopeValue() {
    return child(".nx-tile-content .nx-fieldset:nth-of-type(3) .nx-read-only__data");
  }

  public SelenideElement componentsValue() {
    return child(".nx-tile-content .nx-fieldset:nth-of-type(4) .nx-read-only__data");
  }

  public SelenideElement mixedViolationsAlert() {
    return child("#iq-bulk-waiver-mixed-violations-alert");
  }

  public SelenideElement expirationValue() {
    return child(".nx-tile-content .nx-fieldset:nth-of-type(5) .nx-read-only__data");
  }

  public SelenideElement reasonValue() {
    return child(".nx-tile-content .nx-fieldset:nth-of-type(6) .nx-read-only__data");
  }

  public SelenideElement commentValue() {
    return child(".nx-tile-content .nx-fieldset:nth-of-type(7) .nx-read-only__data");
  }

  public SelenideElement submitError() {
    return child(".nx-footer .nx-alert");
  }

  public Button cancelButton() {
    return new Button(".nx-btn-bar button:nth-child(1)");
  }

  public Button backButton() {
    return new Button(".nx-btn-bar button:nth-child(2)");
  }

  public Button submitButton() {
    return new Button(".nx-btn-bar .nx-btn--primary");
  }
}
