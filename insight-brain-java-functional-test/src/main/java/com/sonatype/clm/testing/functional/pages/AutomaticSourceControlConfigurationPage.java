/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Toggle;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class AutomaticSourceControlConfigurationPage
    extends BasicElement<AutomaticSourceControlConfigurationPage>
{
  public static final String URL = BaseUrl.resolvePageUrl("/automaticSourceControlConfiguration");

  private static final String ROOT_SELECTOR = "#automatic-source-control-configuration";

  public AutomaticSourceControlConfigurationPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child(".iq-tile-header");
  }

  public SelenideElement explanation() {
    return child("#automatic-source-control-explanation");
  }

  public Toggle toggle() {
    return new Toggle(childSelector("#automatic-source-control-toggle-checkbox"));
  }

  public SelenideElement update() {
    return child("#automatic-source-control-update");
  }

  public SelenideElement cancel() {
    return child("#automatic-source-control-cancel");
  }
}
