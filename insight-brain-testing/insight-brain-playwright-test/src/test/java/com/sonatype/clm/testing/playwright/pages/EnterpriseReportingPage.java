/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Enterprise Reporting landing page
 * ({@code enterpriseReporting/EnterpriseReportingLandingPage.jsx}).
 */
public class EnterpriseReportingPage
    extends BasePage
{
  public static final String COPY_SUPPORT_INFO_BUTTON_NAME = "Copy Support Info to Clipboard";

  public static final String COPY_CONFIRMATION_MESSAGE = "Support info copied to clipboard";

  public static final String ENTERPRISE_DASHBOARDS_SECTION_TITLE = "Enterprise Dashboards";

  private static final String LANDING_ROOT_ID = "enterprise-reporting-landing-page";

  private static final String DASHBOARD_ROOT_ID = "enterprise-reporting-dashboard-page";

  /** Always scope to {@link #dashboardPageContainer()} — bare {@code "#dashboard"} is collision-prone. */
  private static final String LOOKER_IFRAME_ID = "dashboard";

  public EnterpriseReportingPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/enterpriseReportingLandingPage";
  }

  public Locator container() {
    return locator("#" + LANDING_ROOT_ID);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  public Locator enterpriseDashboardsSectionHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName(ENTERPRISE_DASHBOARDS_SECTION_TITLE));
  }

  /**
   * Card for a specific dashboard on the Enterprise Reporting landing page.
   * ID selector used: each card is a generic {@code <div>} stamped with a data-driven id;
   * no ARIA role or accessible name is available to scope it.
   *
   * @param dashboardId the dashboard identifier, e.g. {@code "react2shell"} or {@code "sbom-scorecard"}
   */
  public Locator enterpriseDashboardCard(String dashboardId) {
    return container().locator("#enterprise-reporting-dashboard-" + dashboardId);
  }

  public Locator react2ShellCard() {
    return enterpriseDashboardCard("react2shell");
  }

  public Locator react2ShellCardHeading() {
    return react2ShellCard().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(3));
  }

  public Locator react2ShellViewLink() {
    return react2ShellCard().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("View React2Shell Impact"));
  }

  public Locator dashboardCardViewButton(String dashboardId, String accessButtonText) {
    return enterpriseDashboardCard(dashboardId).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(accessButtonText));
  }

  public Locator dashboardPageContainer() {
    return locator("#" + DASHBOARD_ROOT_ID);
  }

  public Locator dashboardSubpageHeading() {
    return dashboardPageContainer().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  /** Non-standard role on element; anchor on id instead. */
  public Locator dashboardIframeContainer() {
    return dashboardPageContainer().locator("#" + LOOKER_IFRAME_ID);
  }

  /** Unlabelled scope container — used only to scope role-based queries. */
  public Locator supportInfoSection() {
    return locator(".iq-enterprise-reporting-support-info");
  }

  public Locator copySupportInfoButton() {
    return supportInfoSection().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(COPY_SUPPORT_INFO_BUTTON_NAME));
  }

  /** Decorative {@code <svg>} (aria-hidden); CSS scope is the only hook. */
  public Locator copySupportInfoIcon() {
    return copySupportInfoButton().locator("svg");
  }

  public Locator copyConfirmationMessage() {
    return supportInfoSection().getByText(COPY_CONFIRMATION_MESSAGE,
        new Locator.GetByTextOptions().setExact(true));
  }

  /** NxLoadError renders as an NxErrorAlert with {@code role="alert"}. */
  public Locator supportInfoLoadError() {
    return supportInfoSection().getByRole(AriaRole.ALERT);
  }

  public Locator supportInfoLoadErrorRetryButton() {
    return supportInfoLoadError().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Retry"));
  }
}
