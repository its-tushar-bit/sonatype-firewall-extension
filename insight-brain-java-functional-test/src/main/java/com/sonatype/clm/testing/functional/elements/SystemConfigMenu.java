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

  public SelenideElement webhooks() {
    return child("#system-configuration-webhooks a");
  }

  public SelenideElement successMetrics() {
    return child("#system-configuration-success-metrics a");
  }
}
