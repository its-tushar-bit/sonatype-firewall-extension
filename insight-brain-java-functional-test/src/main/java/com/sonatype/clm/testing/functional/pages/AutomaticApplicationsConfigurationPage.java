/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.elements.Toggle;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class AutomaticApplicationsConfigurationPage
    extends BasicElement<AutomaticApplicationsConfigurationPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/automaticApplicationsConfiguration");
  }

  private static final String ROOT_SELECTOR = "#automatic-applications-configuration";

  public AutomaticApplicationsConfigurationPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child(".iq-tile-header");
  }

  public SelenideElement explanation() {
    return child("#automatic-applications-explanation");
  }

  public Toggle toggle() {
    return new Toggle(childSelector("#automatic-applications-toggle-checkbox"));
  }

  public Dropdown organization() {
    return new Dropdown(childSelector("#automatic-applications-organization"));
  }

  public SelenideElement update() {
    return child("#automatic-applications-update");
  }

  public SelenideElement cancel() {
    return child("#automatic-applications-cancel");
  }
}
