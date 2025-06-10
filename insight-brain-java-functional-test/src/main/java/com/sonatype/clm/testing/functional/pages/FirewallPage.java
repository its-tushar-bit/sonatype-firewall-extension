/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallMetrics;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallMetricsContent;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallPageTabs;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallQuarantineTable;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallStatus;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallWaiversTable;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.RoiFirewallMetricsTab;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.RoiFirewallMetrics;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
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
    return BaseUrl.resolvePageUrl("/malware-defense/dashboard");
  }
  
  public static String roiTabUrl() {
    return BaseUrl.resolvePageUrl("/malware-defense/dashboard?roiEnabled=true");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public FirewallStatus firewallStatus() {
    return new FirewallStatus(ROOT);
  }

  public FirewallMetrics firewallMetrics() {
    return new FirewallMetrics(ROOT);
  }

  public FirewallMetricsContent firewallMetricsContent(String id) {
    return new FirewallMetricsContent(ROOT, id);
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

  public ElementsCollection getAllLoadingSpinners() {
    return children(".nx-loading-spinner");
  }

  public static String urlToFirewallWaivers() {
    return BaseUrl.resolvePageUrl("malware-defense/dashboard/components/waivers");
  }

  public FirewallWaiversTable firewallWaiversTable() {
    return new FirewallWaiversTable(ROOT);
  }
  
  public RoiFirewallMetricsTab roiFirewallMetricsTab() {
    return new RoiFirewallMetricsTab(ROOT);
  }
  
  public RoiFirewallMetrics roiFirewallMetrics() {
    return new RoiFirewallMetrics(ROOT);
  }

  public FirewallPageTabs firewallPageTabs() {
    return new FirewallPageTabs(ROOT);
  }
}
