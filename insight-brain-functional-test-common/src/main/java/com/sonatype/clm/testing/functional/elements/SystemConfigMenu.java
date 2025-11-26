/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class SystemConfigMenu
    extends BasicElement<SystemConfigMenu>
{
  public SystemConfigMenu() {
    super("#system-configuration-menu");
  }

  public SelenideElement dropdownToggle() {
    return $("#system-configuration-menu > button");
  }

  public SelenideElement users() {
    return $("#system-configuration-users");
  }

  public SelenideElement roles() {
    return $("#system-configuration-roles");
  }

  public SelenideElement administrators() {
    return $("#system-configuration-administrators");
  }

  public SelenideElement productLicense() {
    return $("#system-configuration-product-license");
  }

  public SelenideElement ldap() {
    return $("#system-configuration-ldap");
  }

  public SelenideElement automaticScmConfiguration() {
    return $("#system-configuration-automatic-scm-configuration");
  }

  public SelenideElement webhooks() {
    return child("#system-configuration-webhooks");
  }

  public SelenideElement systemNotice() {
    return child("#system-configuration-system-notice");
  }

  public SelenideElement successMetrics() {
    return child("#system-configuration-success-metrics");
  }

  public SelenideElement automaticApplications() {
    return child("#system-configuration-automatic-applications");
  }

  public SelenideElement emailConfiguration() {
    return child("#system-configuration-email");
  }

  public SelenideElement proxyConfiguration() {
    return child("#system-configuration-proxy");
  }

  public SelenideElement advancedSearchConfiguration() {
    return child("#system-configuration-advanced-search");
  }

  public SelenideElement baseUrlConfiguration() {
    return child("#system-configuration-base-url");
  }

  public SelenideElement samlConfiguration() {
    return child("#system-configuration-saml");
  }

  public SelenideElement userTokensConfiguration() {
    return child("#system-configuration-user-tokens");
  }

  public SelenideElement dataInsights() {
    return child("#system-labs-data-insights");
  }
}
