/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/** Aggregator of system-preferences route URLs and container locators — not a single-screen page object. */
public class SystemPreferencesRoutes
    extends BasePage
{
  public SystemPreferencesRoutes() {
    super();
  }

  public static String mailConfigUrl() {
    return MailConfigurationPage.url();
  }

  public static String systemNoticeUrl() {
    return SystemNoticePage.url();
  }

  public static String waivedComponentUpgradesUrl() {
    return "/assets/index.html#/waivedComponentUpgradesConfiguration";
  }

  public static String roiConfigurationUrl() {
    return RoiConfigurationPage.url();
  }

  public static String automaticApplicationsUrl() {
    return "/assets/index.html#/automaticApplicationsConfiguration";
  }

  public static String automaticSourceControlUrl() {
    return "/assets/index.html#/automaticSourceControlConfiguration";
  }

  public static String zscalerConfigUrl() {
    return "/assets/index.html#/firewall/zscalerConfig";
  }

  public static String gettingStartedUrl() {
    return "/assets/index.html#/gettingStarted";
  }

  public static String dataInsightsUrl() {
    return "/assets/index.html#/dataInsights";
  }

  public static String scmOnboardingUrl() {
    return "/assets/index.html#/onboarding";
  }

  public static String apiDocumentationUrl() {
    return ApiDocumentationPage.url();
  }

  public static String userActivityUrl() {
    return "/assets/index.html#/users/activity";
  }

  public Locator systemNoticeContainer() {
    return locator("#system-notice-configuration");
  }

  public Locator waivedComponentUpgradesContainer() {
    return locator("main#waived-component-upgrades-configuration");
  }

  public Locator roiConfigurationContainer() {
    return locator("#roi-configuration-page");
  }

  public Locator automaticApplicationsContainer() {
    return locator("#auto-app-config-configuration");
  }

  public Locator automaticSourceControlContainer() {
    return locator("#automatic-source-control-configuration-container");
  }

  public Locator automaticSourceControlToggle() {
    return nxToggleLabel("Enable Automatic Source Control Configuration");
  }

  public Locator zscalerConfigContainer() {
    return locator("#zscaler-config-page-container");
  }

  public Locator gettingStartedContainer() {
    return locator("#getting-started");
  }

  public Locator dataInsightsContainer() {
    return locator("#labs-data-insights-container");
  }

  public Locator scmOnboardingContainer() {
    return locator("#scm-onboarding-container");
  }

  public Locator apiDocumentationContainer() {
    return locator("#api-page");
  }

  public Locator userActivityContainer() {
    return locator("#user-management");
  }
}
