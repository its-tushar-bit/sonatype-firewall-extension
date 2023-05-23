/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantine;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineStatus;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallQuarantineStatus;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallQuarantineTable;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallStatus;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class FirewallPage
    extends BasicElement<FirewallPage>
{
  public static final String ROOT = "#firewall-page";

  public static final String CHILD_HEADER_CSS_CLASS = ".nx-h3";

  public FirewallPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/firewall");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public FirewallStatus firewallStatus() {
    return new FirewallStatus(ROOT);
  }

  public FirewallQuarantineStatus firewallQuarantineStatus() {
    return new FirewallQuarantineStatus(ROOT);
  }

  public FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus() {
    return new FirewallAutoUnquarantineStatus(ROOT);
  }

  public FirewallAutoUnquarantine firewallQuarantine() {
    return new FirewallAutoUnquarantine(ROOT);
  }

  public FirewallAutoUnquarantine firewallAutoReleaseQuarantine() {
    return new FirewallAutoUnquarantine(ROOT);
  }

  public FirewallQuarantineTable firewallQuarantineTable() {
    return new FirewallQuarantineTable();
  }

  public FirewallConfigurationModal firewallConfigurationModal() {
    return new FirewallConfigurationModal(ROOT);
  }

  public FirewallWelcomeModal firewallWelcomeModal() {
    return new FirewallWelcomeModal(ROOT);
  }
}
