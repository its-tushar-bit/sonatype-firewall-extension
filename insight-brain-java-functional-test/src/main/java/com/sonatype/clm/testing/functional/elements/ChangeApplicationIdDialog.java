/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ChangeApplicationIdDialog
    extends BasicElement<ChangeApplicationIdDialog>
{
  private static final String FOOTER_SELECTOR = ".iq-modal-footer";

  public ChangeApplicationIdDialog() {
    super("#change-application-id-modal");
  }

  public SelenideElement body() {
    return child(".iq-modal-content");
  }

  public SelenideElement currentId() {
    return $("#editor-current-id");
  }

  public SelenideElement newId() {
    return $("#editor-new-id");
  }

  public SelenideElement changeButton() {
    return child(FOOTER_SELECTOR, ".iq-btn.iq-btn--primary");
  }
}

