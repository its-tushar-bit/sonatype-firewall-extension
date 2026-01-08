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

  public SelenideElement autoUnquarantineToggleIntegrityRating() {
    return child("#auto-unquarantine-toggle-integrity-rating");
  }

  public NxCheckbox autoUnquarantineCheckBoxIntegrityRating() {
    return new NxCheckbox(autoUnquarantineToggleIntegrityRating());
  }

  public SelenideElement autoUnquarantineToggleWithIndex(int index) {
    return child("#auto-release-condition-toggles .nx-toggle:nth-of-type(" + index + ")");
  }

  public NxCheckbox autoUnquarantineCheckBoxWithIndex(int index) {
    return new NxCheckbox(autoUnquarantineToggleWithIndex(index));
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

  public SelenideElement infoAlert() {
    return child(".nx-alert--info");
  }

  public SelenideElement readMoreLink() {
    return child(".nx-alert--info a.nx-text-link--external");
  }
}
