/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ImportPolicyModal
{
  public static final String ROOT_SELECTOR = "#import-policy-modal";

  public static SelenideElement root() {
    return $(ROOT_SELECTOR);
  }

  public static SelenideElement fileInput() {
    return $(ROOT_SELECTOR + " .nx-file-upload__input");
  }

  public static SelenideElement fileInputClearButton() {
    return $(ROOT_SELECTOR + " .nx-selected-file__dismiss-btn");
  }

  public static SelenideElement fileInputRequiredFieldError() {
    return $(ROOT_SELECTOR + " .nx-field-validation-message");
  }

  public static SelenideElement importButton() {
    return $(ROOT_SELECTOR + " .nx-form__submit-btn");
  }

  public static SelenideElement errorMessage() {
    return $(ROOT_SELECTOR + " .nx-load-error__message");
  }

  public static SelenideElement errorRetryButton() {
    return $(ROOT_SELECTOR + " .nx-load-error__retry");
  }
}
