/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;

import com.codeborne.selenide.SelenideElement;

public class EditAttributionModal
    extends BasicElement<EditCopyrightsModal>
{
  public EditAttributionModal() {
    super("#edit-attribution-modal");
  }

  public SelenideElement header() {
    return child("h2");
  }

  public SelenideElement attributionText() {
    return child("textarea");
  }

  public SelenideElement scopeDropdown() {
    return child("#edit-attribution-scope-selection");
  }

  public Button save() {
    return new Button(childSelector(".nx-btn--primary"));
  }

  public Button cancel() {
    return new Button(childSelector(".nx-form__cancel-btn"));
  }
}
