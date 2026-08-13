/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class FormUtils
{
  public static final String DEFAULT_VALIDATION_ERRORS_PREFIX = "There were validation errors.";

  public static final String DEFAULT_NO_CHANGES_TO_SAVE = "There are no changes to save.";

  public static final String DEFAULT_ERROR_SAVING_DATA_PREFIX = "An error occurred saving data.";

  private static final String VALIDATION_ERRORS_ALERT_CONTAINER_SELECTOR = ".nx-form__validation-errors";

  private static final String VALIDATION_ERRORS_ERROR_CONTAINER_SELECTOR = ".nx-alert--error";

  private static final String VALIDATION_ERRORS_ALERT_CONTENT_SELECTOR = ".nx-alert__content";

  private static final String VALIDATION_ERRORS_ALERT_RETRY_BUTTON_SELECTOR = ".nx-load-error__retry";

  public static SelenideElement getRetryButton() {
    return $(VALIDATION_ERRORS_ERROR_CONTAINER_SELECTOR).$(VALIDATION_ERRORS_ALERT_RETRY_BUTTON_SELECTOR);
  }

  public static SelenideElement getErrorElement() {
    return $(VALIDATION_ERRORS_ERROR_CONTAINER_SELECTOR).$(VALIDATION_ERRORS_ALERT_CONTENT_SELECTOR);
  }

  public static SelenideElement getErrorElement(BasicElement<?> rootElement) {
    return rootElement.getElement()
        .$(VALIDATION_ERRORS_ERROR_CONTAINER_SELECTOR)
        .$(VALIDATION_ERRORS_ALERT_CONTENT_SELECTOR);
  }

  public static SelenideElement getErrorElement(SelenideElement rootElement) {
    return rootElement.$(VALIDATION_ERRORS_ERROR_CONTAINER_SELECTOR).$(VALIDATION_ERRORS_ALERT_CONTENT_SELECTOR);
  }

  public static SelenideElement getAlertElement() {
    return $(VALIDATION_ERRORS_ALERT_CONTAINER_SELECTOR).$(VALIDATION_ERRORS_ALERT_CONTENT_SELECTOR);
  }

  public static SelenideElement getAlertElement(BasicElement<?> rootElement) {
    return rootElement.getElement()
        .$(VALIDATION_ERRORS_ALERT_CONTAINER_SELECTOR)
        .$(VALIDATION_ERRORS_ALERT_CONTENT_SELECTOR);
  }

  public static SelenideElement getAlertElement(SelenideElement rootElement) {
    return rootElement.$(VALIDATION_ERRORS_ALERT_CONTAINER_SELECTOR).$(VALIDATION_ERRORS_ALERT_CONTENT_SELECTOR);
  }
}
