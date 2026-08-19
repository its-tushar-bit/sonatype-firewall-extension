/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class SidebarNavigation
{
  public static SelenideElement container() {
    return $(".nx-global-sidebar-2");
  }

  public static SelenideElement sidebarLinks() {
    return $(".nx-global-sidebar-2__nav");
  }

  public static SelenideElement productVersion() {
    return $(".nx-global-footer-2");
  }

  public static SelenideElement productLogo() {
    return $(".nx-global-header-2__logo");
  }

  public static SelenideElement productInfoLink() {
    return $(".nx-global-sidebar__product-info");
  }

  public static SelenideElement toggleNavigationButton() {
    return $(".nx-global-sidebar-2__toggle");
  }

  public static void closeNavigationSidebar() {
    SelenideElement sidebarToggle = toggleNavigationButton();
    if (container().has(CLM.CSS_SIDEBAR_CLOSED)) {
      return;
    }

    sidebarToggle.click();
  }

  public static void openNavigationSidebar() {
    SelenideElement sidebarToggle = toggleNavigationButton();
    if (container().has(CLM.CSS_SIDEBAR_OPEN)) {
      return;
    }

    sidebarToggle.click();
  }

  public static SidebarNavigationButton dashboardNavigationButton() {
    return new SidebarNavigationButton("#dashboard-navigation-button");
  }

  public static SidebarNavigationButton reportingNavigationButton() {
    return new SidebarNavigationButton("#reporting-navigation-button");
  }

  public static SidebarNavigationButton policiesNavigationButton() {
    return new SidebarNavigationButton("#policies-navigation-button");
  }

  public static SidebarNavigationButton labsNavigationButton() {
    return new SidebarNavigationButton("#labs-navigation-button");
  }

  public static SidebarNavigationButton vulnerabilityDetailsNavigationButton() {
    return new SidebarNavigationButton("#vulnerability-navigation-button");
  }

  public static SidebarNavigationButton advancedSearchNavigationButton() {
    return new SidebarNavigationButton("#search-navigation-button");
  }

  public static SidebarNavigationButton firewallNavigationButton() {
    return new SidebarNavigationButton("#firewall-navigation-button");
  }

  public static SidebarNavigationButton legalNavigationButton() {
    return new SidebarNavigationButton("#advanced-legal-navigation-button");
  }

  public static SidebarNavigationButton developerApiNavigationButton() {
    return new SidebarNavigationButton("#sonatype-developer-api-navigation-button");
  }

  public static SidebarNavigationButton lifecycleApiNavigationButton() {
    return new SidebarNavigationButton("#api-navigation-button");
  }

  public static SidebarNavigationButton firewallApiNavigationButton() {
    return new SidebarNavigationButton("#sonatype-firewall-api-navigation-button");
  }

  public static SidebarNavigationButton sbomManagerApiNavigationButton() {
    return new SidebarNavigationButton("#sbom-manager-api-navigation-button");
  }

  public static SidebarNavigationButton enterpriseReportingNavigationButton() {
    return new SidebarNavigationButton("#enterprise-reporting-button");
  }

  public static SidebarNavigationButton firewallDashboardNavigationButton() {
    return new SidebarNavigationButton("#sonatype-firewall-dashboard-navigation-button");
  }

  public static SidebarNavigationButton firewallRepositoriesNavigationButton() {
    return new SidebarNavigationButton("#sonatype-firewall-repositories-navigation-button");
  }

  public static SidebarNavigationButton firewallReportsNavigationButton() {
    return new SidebarNavigationButton("#sonatype-firewall-reports-navigation-button");
  }

  public static SidebarNavigationButton sbomManagerDashboardNavigationButton() {
    return new SidebarNavigationButton("#sbom-manager-dashboard-navigation-button");
  }

  public static SidebarNavigationButton sbomManagerOrganizationsNavigationButton() {
    return new SidebarNavigationButton("#sbom-manager-organizations-navigation-button");
  }
}
