/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class SourceControlRepositoryUrlUpdateModal
{
  public static SelenideElement root() {
    return $("#update-source-control-url-modal");
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

  public static SelenideElement continueButton() {
    return root().$(".nx-form__submit-btn");
  }

  public static SelenideElement cancelButton() {
    return root().$(".nx-form__cancel-btn");
  }
}
