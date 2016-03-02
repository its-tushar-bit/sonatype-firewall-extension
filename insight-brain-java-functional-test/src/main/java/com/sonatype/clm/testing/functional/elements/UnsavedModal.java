/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.selector;

public class UnsavedModal
    extends BasicElement<UnsavedModal>
{
  public UnsavedModal() {
    super(".unsaved-modal");
  }
  
  public SelenideElement continueButton() {
    return $(selector(selector, ".btn-primary"));
  }

  public SelenideElement cancelButton() {
    return $(selector(selector, ".btn:not(.btn-primary)"));
  }
}
