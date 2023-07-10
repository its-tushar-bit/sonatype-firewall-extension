/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;

import com.codeborne.selenide.SelenideElement;

public class IncompleteConfigurationModal
    extends BasicElement<IncompleteConfigurationModal>
{
  public IncompleteConfigurationModal(String rootSelector) {
    super(rootSelector, "#incomplete-configuration-modal");
  }

  public Button continueButton() {
    return new Button(childSelector("#incomplete-configuration-modal-continue-button"));
  }

  public Button cancelButton() {
    return new Button(childSelector("#incomplete-configuration-modal-cancel-button"));
  }

  public SelenideElement modalContent() {
    return child(".nx-modal-content");
  }
}
