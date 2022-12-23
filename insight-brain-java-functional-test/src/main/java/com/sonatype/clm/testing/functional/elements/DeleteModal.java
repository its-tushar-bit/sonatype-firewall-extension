/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class DeleteModal
{
  public static Condition headerText(String resourceType) {
    return text("Delete " + resourceType);
  }

  public static Condition bodyText(String resourceName) {
    return text("You are about to permanently remove " + resourceName + ". This action cannot be undone.");
  }

  public static SelenideElement root() {
    return $("#delete-modal");
  }

  public static SelenideElement header() {
    return root().$(".iq-modal-header");
  }

  public static SelenideElement body() {
    return root().$(".iq-modal-content");
  }

  public static SelenideElement error() {
    return root().$(".iq-alert--error");
  }

  public static SelenideElement continueButton() {
    return root().$(".iq-btn--primary");
  }

  public static SelenideElement cancelButton() {
    return root().$(".iq-btn:not(.iq-btn--primary)[type='button']");
  }
  
  public static SelenideElement retryButton() {
    return root().$(".iq-btn--error");
  }
}
