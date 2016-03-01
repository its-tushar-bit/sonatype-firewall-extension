/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ErrorModal
{
  public static SelenideElement root() {
    return $("#error-modal");
  }

  public static SelenideElement header() {
    return root().$(".clm-modal-header");
  }

  public static SelenideElement body() {
    return root().$(".clm-modal-body");
  }

  public static SelenideElement closeButton() {
    return root().$(".btn");
  }
}
