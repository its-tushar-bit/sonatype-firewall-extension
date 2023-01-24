/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class ProxyConfigurationPage
    extends BasicElement<ProxyConfigurationPage>
{
  public static final String ROOT = "#proxy-config-container";

  public ProxyConfigurationPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/proxyConfig");
  }

  public SelenideElement title() {
    return child(".nx-h2");
  }

  public SelenideElement hostName() {
    return child("#proxy-config-hostname");
  }

  public SelenideElement port() {
    return child("#proxy-config-port");
  }

  public SelenideElement username() {
    return child("#proxy-config-username");
  }

  public SelenideElement password() {
    return child("#proxy-config-password");
  }

  public SelenideElement excludeHosts() {
    return child("#proxy-config-exclude-hosts");
  }

  public SelenideElement save() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child(".nx-form__cancel-btn");
  }

  public SelenideElement delete() {
    return child("#proxy-config-delete");
  }

  public SelenideElement loadError() {
    return child(".nx-load-error__message");
  }

  public SelenideElement productLicenseNavigation() {
    return child("#proxy-config-product-license-navigation");
  }
}
