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

public class AutomaticSourceControlConfigurationPage
    extends BasicElement<AutomaticSourceControlConfigurationPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/automaticSourceControlConfiguration");
  }

  private static final String ROOT_SELECTOR = "#automatic-source-control-configuration";

  public AutomaticSourceControlConfigurationPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child(".nx-tile-header");
  }

  public SelenideElement explanation() {
    return child("#automatic-source-control-explanation");
  }

  public SelenideElement explanationAutomaticApplications() {
    return child("#automatic-source-control-automatic-applications-explanation");
  }

  public NxToggle toggle() {
    return new NxToggle(childSelector("#automatic-source-control-toggle-checkbox"));
  }

  public SelenideElement update() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child("#automatic-source-control-cancel");
  }
}
