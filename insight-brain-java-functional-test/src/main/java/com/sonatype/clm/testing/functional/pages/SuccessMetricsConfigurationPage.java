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
    return child(".iq-tile-header");
  }

  public SelenideElement explanation() {
    return child("#success-metrics-explanation");
  }

  public Toggle toggle() {
    return new Toggle(childSelector("#success-metrics-toggle"));
  }

  public SelenideElement update() {
    return child("#success-metrics-update");
  }

  public SelenideElement cancel() {
    return child("#success-metrics-cancel");
  }
}
