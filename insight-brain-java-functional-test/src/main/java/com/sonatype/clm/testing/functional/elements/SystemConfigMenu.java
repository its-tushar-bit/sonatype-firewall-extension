/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class SystemConfigMenu
    extends BasicElement<SystemConfigMenu>
{
  public SystemConfigMenu() {
    super("#system-configuration-menu");
  }

  public SelenideElement dropdownToggle() {
    return child("#system-configuration-menu-dropdown-toggle");
  }

  public SelenideElement users() {
    return child("#system-configuration-users a");
  }

  public SelenideElement roles() {
    return child("#system-configuration-roles a");
  }

  public SelenideElement administrators() {
    return child("#system-configuration-administrators a");
  }

  public SelenideElement productLicense() {
    return child("#system-configuration-product-license a");
  }

  public SelenideElement ldap() {
    return child("#system-configuration-ldap a");
  }

  public SelenideElement webhooks() {
    return child("#system-configuration-webhooks a");
  }

  public SelenideElement systemNotice() {
    return child("#system-configuration-system-notice a");
  }

  public SelenideElement successMetrics() {
    return child("#system-configuration-success-metrics a");
  }

  public SelenideElement automaticApplications() {
    return child("#system-configuration-automatic-applications a");
  }

  public SelenideElement emailConfiguration() {
    return child("#system-configuration-email a");
  }

  public SelenideElement proxyConfiguration() {
    return child("#system-configuration-proxy a");
  }

  public SelenideElement advancedSearchConfiguration() {
    return child("#system-configuration-advanced-search a");
  }

  public SelenideElement scmOnboarding() {
    return child("#system-configuration-scm-onboarding a");
  }

  public SelenideElement dataInsights() {
    return child("#system-labs-data-insights a");
  }
}
