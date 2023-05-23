/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;

import com.codeborne.selenide.SelenideElement;

public class FirewallWelcomeModal
    extends BasicElement<FirewallWelcomeModal>
{
  public FirewallWelcomeModal(String rootSelector) {
    super(rootSelector, "#firewall-welcome-modal");
  }

  public Button closeButton() {
    return new Button(childSelector(".nx-btn--secondary"));
  }

  public SelenideElement modalContent() {
    return child(".nx-modal-content");
  }
}
