/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class DisplayThemeModal
    extends BasicElement<DisplayThemeModal>
{
  public DisplayThemeModal() {
    super("#iq-display-theme-modal");
  }

  public SelenideElement systemSettingRadio() {
    return $("#iq-system-setting-radio");
  }

  public SelenideElement darkModeRadio() {
    return $("#iq-dark-mode-radio");
  }

  public SelenideElement lightModeRadio() {
    return $("#iq-light-mode-radio");
  }

  public SelenideElement closeButton() {
    return $("#iq-display-theme-modal .nx-btn");
  }
}
