/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;

import com.codeborne.selenide.SelenideElement;

public class FirewallConfigurationModal
    extends BasicElement<FirewallConfigurationModal>
{
  public FirewallConfigurationModal(String rootSelector) {
    super(rootSelector, "#firewall-configuration-modal");
  }

  public SelenideElement autoUnquarantineToggle() {
    return child("#auto-unquarantine-toggle");
  }

  public NxCheckbox autoUnquarantineCheckBox() {
    return new NxCheckbox(autoUnquarantineToggle());
  }

  public Button saveButton() {
    return new Button(childSelector(".nx-btn--primary"));
  }

  public Button cancelButton() {
    return new Button(childSelector(".nx-btn:not(.nx-btn--primary)[type='button']"));
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }

  public Button retryButton() {
    return new Button(childSelector(".nx-load-error__retry"));
  }

  public SelenideElement modalContent() {
    return child(".nx-modal-content");
  }
}
