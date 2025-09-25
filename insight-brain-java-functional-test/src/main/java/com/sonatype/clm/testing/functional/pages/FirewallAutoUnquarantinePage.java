/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineMtd;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineStatus;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineYtd;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallUnquarantineTable;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class FirewallAutoUnquarantinePage
    extends BasicElement<FirewallAutoUnquarantinePage>
{
  public static final String ROOT = "#firewall-auto-unquarantine-page";

  public static final String CHILD_HEADER_CSS_CLASS = ".nx-h3";

  public FirewallAutoUnquarantinePage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/firewall/autoReleaseQuarantine");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public NxBackButton backToFirewallButton() {
    return new NxBackButton();
  }

  public FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus() {
    return new FirewallAutoUnquarantineStatus(ROOT);
  }

  public FirewallAutoUnquarantineMtd firewallAutoReleaseQuarantineMtd() {
    return new FirewallAutoUnquarantineMtd(ROOT);
  }

  public FirewallAutoUnquarantineYtd firewallAutoReleaseQuarantineYtd() {
    return new FirewallAutoUnquarantineYtd(ROOT);
  }

  public FirewallUnquarantineTable firewallUnquarantineTable() {
    return new FirewallUnquarantineTable(ROOT);
  }

  public FirewallConfigurationModal firewallConfigurationModal() {
    return new FirewallConfigurationModal(ROOT);
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }

  public Button retryButton() {
    return new Button(childSelector(".nx-load-error__retry"));
  }
}
