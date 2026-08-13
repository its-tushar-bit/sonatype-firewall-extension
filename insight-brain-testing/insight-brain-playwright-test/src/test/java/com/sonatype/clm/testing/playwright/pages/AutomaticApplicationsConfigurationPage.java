/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Automatic Application Creation configuration page ({@code /automaticApplicationsConfiguration}).
 * The page container is {@code <main className="nx-page-main">} without an id; assertions
 * anchor on the {@code #auto-app-config-configuration} tile and the H1 heading text.
 */
public class AutomaticApplicationsConfigurationPage
    extends BasePage
{
  private static final String TILE = "#auto-app-config-configuration";

  private static final String TOGGLE_INPUT = "#auto-app-config-toggle-checkbox";

  private static final String SOURCE_CONTROL_EXPLANATION = "#auto-app-config-source-control-explanation";

  private static final String SCM_ONBOARDING_EXPLANATION = "#auto-app-config-scm-onboarding-explanation";

  public AutomaticApplicationsConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/automaticApplicationsConfiguration";
  }

  public Locator tile() {
    return locator(TILE);
  }

  public Locator pageHeading() {
    // "Automatic Applications" (H1) is a prefix of "Configure Automatic Applications" (H2)
    // so setName alone matches both — pin to level=1 with setExact to disambiguate.
    return page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setLevel(1)
            .setName("Automatic Applications")
            .setExact(true));
  }

  public Locator tileHeading() {
    return tile().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(2).setName("Configure Automatic Applications"));
  }

  public Locator enabledToggleLabel() {
    return nxToggleLabel(tile(), "Automatic Application Creation");
  }

  public Locator enabledToggleInput() {
    return locator(TOGGLE_INPUT);
  }

  public Locator updateButton() {
    return tile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update").setExact(true));
  }

  public Locator cancelButton() {
    return tile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Cancel").setExact(true));
  }

  public Locator parentOrganizationSelect() {
    return byLabel("Parent Organization");
  }

  public Locator sourceControlExplanation() {
    return locator(SOURCE_CONTROL_EXPLANATION);
  }

  public Locator scmOnboardingExplanation() {
    return locator(SCM_ONBOARDING_EXPLANATION);
  }

  public Locator automaticSourceControlLink() {
    return sourceControlExplanation().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Automatic Source Control"));
  }

  public Locator scmOnboardingLink() {
    return scmOnboardingExplanation().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Easy SCM Onboarding"));
  }
}
