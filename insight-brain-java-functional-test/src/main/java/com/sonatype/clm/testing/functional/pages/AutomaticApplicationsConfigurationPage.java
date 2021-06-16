/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxToggle;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class AutomaticApplicationsConfigurationPage
    extends BasicElement<AutomaticApplicationsConfigurationPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/automaticApplicationsConfiguration");
  }

  private static final String ROOT_SELECTOR = "#auto-app-config-configuration";

  public AutomaticApplicationsConfigurationPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child(".nx-tile-header");
  }

  public SelenideElement explanation() {
    return child("#auto-app-config-explanation");
  }

  public NxToggle toggle() {
    return new NxToggle(childSelector("#auto-app-config-toggle-checkbox"));
  }

  public NxFormSelect organization() {
    return new NxFormSelect(childSelector("#parent-organization-selector"));
  }

  public SelenideElement update() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child("#auto-app-config-cancel");
  }
}
