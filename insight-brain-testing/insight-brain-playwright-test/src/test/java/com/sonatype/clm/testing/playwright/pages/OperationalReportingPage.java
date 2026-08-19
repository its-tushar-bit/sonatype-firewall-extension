/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** Operational Reporting landing page — shown when {@code integrated-enterprise-reporting} is absent. */
public class OperationalReportingPage
    extends BasePage
{
  private static final String ROOT = "#operational-reporting-landing-page";

  private static final String DESCRIPTION_PREFIX =
      "Operational Reporting provides immediate, real-time insight";

  public OperationalReportingPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/operationalReporting";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageTitle() {
    return locator(ROOT + " #operational-reporting-landing-page-title");
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(1).setName("Operational Reporting"));
  }

  public Locator pageDescription() {
    return locator(ROOT + " #operational-reporting-landing-page-description");
  }

  public Locator pageDescriptionParagraph() {
    return pageDescription().getByText(DESCRIPTION_PREFIX);
  }

  public Locator rapidResponseReportsHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(2).setName("Rapid Response Reports"));
  }

  public Locator contactUsHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(2).setName("Contact Us"));
  }

  private static final String REACT2SHELL_CARD_HEADING = "React2Shell Impact";

  private static final String REACT2SHELL_VIEW_LINK = "View React2Shell Impact";

  /**
   * Card for a specific dashboard on the Operational Reporting landing page.
   * ID selector used: each card is a generic {@code <div>} stamped with a data-driven id;
   * no ARIA role or accessible name is available to scope it.
   */
  private Locator dashboardCard(String dashboardId) {
    return container().locator("#enterprise-reporting-dashboard-" + dashboardId);
  }

  public Locator react2ShellCard() {
    return dashboardCard("react2shell");
  }

  public Locator react2ShellCardHeading() {
    return react2ShellCard().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(3).setName(REACT2SHELL_CARD_HEADING));
  }

  public Locator react2ShellViewLink() {
    return react2ShellCard().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName(REACT2SHELL_VIEW_LINK));
  }
}
