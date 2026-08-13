/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** Page object for the Sonatype Developer Dashboard (lock screen / Overview / summary toggle). */
public class SonatypeDeveloperPage
    extends BasePage
{
  public static final String DASHBOARD_HEADING = "Dashboard";

  public static final String LOCK_SCREEN_HEADING = "Sonatype Developer";

  public static final String LOCK_SCREEN_ERROR_TEXT = "Sonatype Developer is not enabled.";

  public static final String SUMMARY_HEADING = "Build Stage Risk Monitoring Summary";

  public static final String SUMMARY_DISABLED_ALERT_TEXT =
      "Applications Configuration Build Stage Summary is not enabled.";

  public static final String CI_CARD_NAME = "Sync CI With Sonatype Developer";

  public static final String SCM_CARD_NAME = "SCM Feedback";

  public static final String IDE_CARD_NAME = "Integrate using IDEs";

  public SonatypeDeveloperPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/developer/dashboard";
  }

  public Locator container() {
    return byRole(AriaRole.MAIN);
  }

  /** Lock-screen renders two h1s ("Dashboard" + "Sonatype Developer"); callers must scope by name. */
  public Locator headingByName(String accessibleName) {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(1).setName(accessibleName).setExact(true));
  }

  public Locator lockScreenErrorAlert() {
    return byTestId("iq-integrations__missing-license");
  }

  public Locator summaryHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(2).setName(SUMMARY_HEADING));
  }

  public Locator summaryFilterButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Filter"));
  }

  /** {@code NxInfoAlert} has no role; scope on class + text. {@code .first()} guards against stacked alerts. */
  public Locator summaryDisabledInfoAlert() {
    return container().locator(".nx-alert--info")
        .filter(new Locator.FilterOptions().setHasText(SUMMARY_DISABLED_ALERT_TEXT))
        .first();
  }

  public Locator integrationCard(String accessibleName) {
    return container().getByRole(AriaRole.REGION, new Locator.GetByRoleOptions().setName(accessibleName));
  }

  /** Scoped to {@code .nx-card-container}; otherwise {@code getByRole(REGION)} also matches Risk Monitoring. */
  public Locator allIntegrationCards() {
    return container().locator(".nx-card-container").getByRole(AriaRole.REGION);
  }
}
