/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.time.Duration;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class WaiverConfigurationPage
    extends BasicElement<WaiverConfigurationPage>
{
  public static final String ROOT = ".iq-bulk-waiver-configuration-page";

  public WaiverConfigurationPage() {
    super(ROOT);
  }

  public BulkWaiveTitle title() {
    return new BulkWaiveTitle();
  }

  public SelenideElement tileHeaderTitle() {
    return child(".nx-tile-header__title h2");
  }

  public NxFormSelect scopeDropdown() {
    return new NxFormSelect("#bulk-waiver-scope");
  }

  public SelenideElement exactComponentRadio() {
    return child("#exact-component");
  }

  public SelenideElement allVersionsRadio() {
    return child("#all-versions");
  }

  public SelenideElement expirationSelect() {
    return child("#iq-bulk-waiver-expiry-select");
  }

  public SelenideElement customExpirationDateInput() {
    return child(".iq-bulk-waiver-form__date-input .nx-text-input input");
  }

  public SelenideElement customExpirationValidationMessage() {
    return child(".iq-bulk-waiver-form__date-input .nx-text-input__invalid-message");
  }

  public SelenideElement expirationDaysMessage() {
    return child(".iq-bulk-waiver-form__expiration-days-diff");
  }

  public SelenideElement reasonSelect() {
    return child("#iq-bulk-waiver-reason-select");
  }

  public SelenideElement commentsTextarea() {
    return child(".iq-bulk-waiver-form__comments .nx-text-input textarea");
  }

  public Button cancelButton() {
    return new Button(".nx-btn-bar button:nth-child(1)");
  }

  public Button backButton() {
    return new Button(".nx-btn-bar button:nth-child(2)");
  }

  public Button nextButton() {
    return new Button(".nx-btn-bar button:nth-child(3)");
  }

  public static SelenideElement pageLoadSpinner() {
    return $(".nx-loading-spinner");
  }

  public static void waitUntilSpinnersGone() {
    pageLoadSpinner().shouldNotBe(visible, Duration.ofSeconds(10));
  }
}
