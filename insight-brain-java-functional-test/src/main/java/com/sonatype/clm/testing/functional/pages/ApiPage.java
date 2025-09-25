/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class ApiPage
    extends BasicElement<ApiPage>
{
  public static final String ROOT = "#api-page";

  public ApiPage() {
    super(ROOT);
  }

  public static String developerUrl() {
    return BaseUrl.resolvePageUrl("/developer/api");
  }

  public static String lifecycleUrl() {
    return BaseUrl.resolvePageUrl("/api");
  }

  public static String firewallUrl() {
    return BaseUrl.resolvePageUrl("/firewall/api");
  }

  public static String sbomManagerUrl() {
    return BaseUrl.resolvePageUrl("/sbomManager/api");
  }

  public SelenideElement publicTab() {
    return tabAt(1);
  }

  public SelenideElement experimentalTab() {
    return tabAt(2);
  }

  public SelenideElement tabAt(int index) {
    return child(String.format(".nx-tab-list li:nth-child(%d)", index));
  }

  public SelenideElement swaggerUi() {
    return child(".swagger-ui");
  }
}
