/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Operational Reporting landing page — default-license branch (Enterprise Reporting requires
 * {@code integrated-enterprise-reporting}).
 */
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

  /** Wrapper has no role; assertions on the paragraph use {@link #pageDescriptionParagraph()}. */
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

}
