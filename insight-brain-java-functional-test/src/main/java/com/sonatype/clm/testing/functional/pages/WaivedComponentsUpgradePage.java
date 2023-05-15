/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxToggle;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class WaivedComponentsUpgradePage
    extends BasicElement<WaivedComponentsUpgradePage>
{
  private static final String ROOT_SELECTOR = "#waived-component-upgrades-configuration";

  public WaivedComponentsUpgradePage() {
    super(ROOT_SELECTOR);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/waivedComponentUpgradesConfiguration");
  }

  public NxToggle toggle() {
    return new NxToggle(childSelector("#waived-component-upgrade-toggle"));
  }

  public NxToggle toggleInput() {
    return new NxToggle(childSelector("#waived-component-upgrade-toggle input"));
  }

  public SelenideElement update() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child("#waived-component-upgrade-cancel");
  }

  public SelenideElement validationErrors() {
    return child(".nx-form__validation-errors");
  }
}
