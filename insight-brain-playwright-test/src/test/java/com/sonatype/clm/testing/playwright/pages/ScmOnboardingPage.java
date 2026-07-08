/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class ScmOnboardingPage
    extends BasePage
{
  private static final String ROOT = "#scm-onboarding-container";

  public ScmOnboardingPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/onboarding";
  }

  public static String urlForOrg(String organizationId) {
    return "/assets/index.html#/onboarding/" + organizationId;
  }

  public Locator container() {
    return locator(ROOT);
  }

  /**
   * Target Organization dropdown. Anchored on id because the dropdown's accessible name is
   * state-dependent ("Loading..." / "Select" / current org name).
   */
  public Locator targetOrganizationDropdown() {
    return container().locator("#iq-scm-target-organization");
  }

  /** "New Organization" button that opens OwnerModal. */
  public Locator newOrganizationButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("New Organization"));
  }

  /**
   * SCM-token-not-configured error. The repo-table {@code LoadWrapper} renders one of four
   * possible alerts, so we filter on the leading copy fragment to avoid false-greens on the
   * other three.
   */
  public Locator scmTokenNotConfiguredError() {
    return container().locator("#scm-repo-table")
        .getByRole(AriaRole.ALERT)
        .filter(new Locator.FilterOptions()
            .setHasText("Source control authentication is not configured"));
  }
}
