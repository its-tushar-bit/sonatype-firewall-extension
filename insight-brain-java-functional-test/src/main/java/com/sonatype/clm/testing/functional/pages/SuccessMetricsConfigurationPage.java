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

public class SuccessMetricsConfigurationPage
    extends BasicElement<SuccessMetricsConfigurationPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/successMetricsConfiguration");
  }

  private static final String ROOT_SELECTOR = "#success-metrics-configuration";

  public SuccessMetricsConfigurationPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child(".nx-tile-header");
  }

  public SelenideElement explanation() {
    return child(".nx-toggle__content");
  }

  public NxToggle toggle() {
    return new NxToggle(childSelector("#success-metrics-toggle"));
  }

  public SelenideElement update() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child("#success-metrics-cancel");
  }
}
