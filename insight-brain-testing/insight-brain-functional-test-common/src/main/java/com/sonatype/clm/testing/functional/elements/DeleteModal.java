/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class DeleteModal
{
  public static WebElementCondition headerText(String resourceType) {
    return text("Delete " + resourceType);
  }

  public static WebElementCondition bodyText(String resourceName) {
    return text("You are about to permanently remove " + resourceName + ". This action cannot be undone.");
  }

  public static SelenideElement root() {
    return $("#reset-source-control-modal");
  }

  public static SelenideElement header() {
    return root().$(".nx-modal-header");
  }

  public static SelenideElement body() {
    return root().$(".nx-modal-content");
  }

  public static SelenideElement warning() {
    return root().$(".nx-alert--warning");
  }

  public static SelenideElement error() {
    return root().$(".nx-alert--error");
  }

  public static SelenideElement continueButton() {
    return root().$(".nx-form__submit-btn");
  }

  public static SelenideElement cancelButton() {
    return root().$(".nx-form__cancel-btn");
  }

  public static SelenideElement retryButton() {
    return root().$(".nx-load-error__retry");
  }
}
