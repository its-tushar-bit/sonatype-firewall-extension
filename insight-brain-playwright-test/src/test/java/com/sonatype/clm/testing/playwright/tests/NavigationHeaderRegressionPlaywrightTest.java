/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderRegressionAssertions;
import com.sonatype.clm.testing.playwright.pages.HeaderRegressionComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Regression tests for Navigation Header dropdown menus.
 *
 * <p>
 * Tier Badge cannot be automated — {@code TierBadge.jsx} exists on an unmerged branch
 * and the {@code .iq-tier-badge} element does not render in the current production build.
 */
public class NavigationHeaderRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  @Before
  public void openDashboardAndLogin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  /**
   * Help menu dropdown opens and shows the three expected support links.
   * Divergence: "version info" link absent; "Documentation" label → "Online Help".
   */
  @Test
  @Category(RegressionTest.class)
  public void testHeaderHelpMenu_dropdownOpensWithLinks() {
    HeaderRegressionComponent header = new HeaderRegressionComponent();
    header.helpMenuButton().click();

    HeaderRegressionAssertions assertions = new HeaderRegressionAssertions(header);
    assertions.shouldShowHelpMenuDropdown();
    assertions.shouldShowGettingStartedLink();
    assertions.shouldShowOnlineHelpLink();
    assertions.shouldShowRequestSupportLink();
  }

  /**
   * System Preferences dropdown shows all expected config sections.
   * Divergence: manual says "User Tokens"; live label is "User Tokens Configuration".
   */
  @Test
  @Category(RegressionTest.class)
  public void testHeaderSystemPreferencesMenu_dropdownOpensWithAllSections() {
    HeaderRegressionComponent header = new HeaderRegressionComponent();
    header.systemConfigMenuButton().click();

    HeaderRegressionAssertions assertions = new HeaderRegressionAssertions(header);
    assertions.shouldShowSystemConfigMenuLink("Users");
    assertions.shouldShowSystemConfigMenuLink("Roles");
    assertions.shouldShowSystemConfigMenuLink("Administrators");
    assertions.shouldShowSystemConfigMenuLink("Product License");
    assertions.shouldShowSystemConfigMenuLink("LDAP");
    assertions.shouldShowSystemConfigMenuLink("SAML");
    assertions.shouldShowSystemConfigMenuLink("Email");
    assertions.shouldShowSystemConfigMenuLink("Proxy");
    assertions.shouldShowSystemConfigMenuLink("Webhooks");
    assertions.shouldShowSystemConfigMenuLink("System Notice");
    assertions.shouldShowSystemConfigMenuLink("Success Metrics");
    assertions.shouldShowSystemConfigMenuLink("Automatic Applications");
    assertions.shouldShowSystemConfigMenuLink("Base URL");
    assertions.shouldShowSystemConfigMenuLink("Advanced Search");
    assertions.shouldShowSystemConfigMenuLink("User Tokens Configuration");
  }

}
