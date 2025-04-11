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
    return $(".nx-global-sidebar");
  }

  public static SelenideElement mainHeaderButtons() {
    return $("#global-sidebar-buttons");
  }

  public static SelenideElement productVersion() {
    return $(".nx-global-sidebar__release");
  }

  public static SelenideElement productLogo() {
    return $(".nx-global-sidebar__logo");
  }

  public static SelenideElement productInfoLink() {
    return $(".nx-global-sidebar__product-info");
  }

  public static SelenideElement toggleNavigationButton() {
    return $(".nx-global-sidebar__toggle");
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

  public static MainHeaderNavigationButton dashboardNavigationButton() {
    return new MainHeaderNavigationButton("#dashboard-navigation-button");
  }

  public static MainHeaderNavigationButton reportingNavigationButton() {
    return new MainHeaderNavigationButton("#reporting-navigation-button");
  }

  public static MainHeaderNavigationButton policiesNavigationButton() {
    return new MainHeaderNavigationButton("#policies-navigation-button");
  }

  public static MainHeaderNavigationButton labsNavigationButton() {
    return new MainHeaderNavigationButton("#labs-navigation-button");
  }

  public static MainHeaderNavigationButton vulnerabilityDetailsNavigationButton() {
    return new MainHeaderNavigationButton("#vulnerability-navigation-button");
  }

  public static MainHeaderNavigationButton advancedSearchNavigationButton() {
    return new MainHeaderNavigationButton("#search-navigation-button");
  }

  public static MainHeaderNavigationButton firewallNavigationButton() {
    return new MainHeaderNavigationButton("#firewall-navigation-button");
  }

  public static MainHeaderNavigationButton legalNavigationButton() {
    return new MainHeaderNavigationButton("#advanced-legal-navigation-button");
  }

  public static MainHeaderNavigationButton developerApiNavigationButton() {
    return new MainHeaderNavigationButton("#sonatype-developer-api-navigation-button");
  }

  public static MainHeaderNavigationButton lifecycleApiNavigationButton() {
    return new MainHeaderNavigationButton("#api-navigation-button");
  }

  public static MainHeaderNavigationButton firewallApiNavigationButton() {
    return new MainHeaderNavigationButton("#sonatype-firewall-api-navigation-button");
  }

  public static MainHeaderNavigationButton sbomManagerApiNavigationButton() {
    return new MainHeaderNavigationButton("#sbom-manager-api-navigation-button");
  }

  public static MainHeaderNavigationButton enterpriseReportingNavigationButton() {
    return new MainHeaderNavigationButton("#enterprise-reporting-buttonn");
  }

  public static MainHeaderNavigationButton firewallDashboardNavigationButton() {
    return new MainHeaderNavigationButton("#sonatype-firewall-dashboard-navigation-button");
  }

  public static MainHeaderNavigationButton firewallRepositoriesNavigationButton() {
    return new MainHeaderNavigationButton("#sonatype-firewall-repositories-navigation-button");
  }

  public static MainHeaderNavigationButton firewallReportsNavigationButton() {
    return new MainHeaderNavigationButton("#sonatype-firewall-reports-navigation-button");
  }

  public static MainHeaderNavigationButton sbomManagerDashboardNavigationButton() {
    return new MainHeaderNavigationButton("#sbom-manager-dashboard-navigation-button");
  }

  public static MainHeaderNavigationButton sbomManagerOrganizationsNavigationButton() {
    return new MainHeaderNavigationButton("#sbom-manager-organizations-navigation-button");
  }
}
