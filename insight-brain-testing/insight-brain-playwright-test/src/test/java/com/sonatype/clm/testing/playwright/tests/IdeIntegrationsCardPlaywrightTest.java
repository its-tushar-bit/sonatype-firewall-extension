/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.IdeIntegrationsCardPage;
import com.sonatype.clm.testing.playwright.pages.IdeIntegrationsCardPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperPage;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** Tests for the IDE Integrations card on the Developer Dashboard. */
public class IdeIntegrationsCardPlaywrightTest
    extends AbstractIqUiTest
{
  @Before
  public void openDeveloperPage() {
    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testIdeCard_rendersWithHeadingAndFooterLink() {
    IdeIntegrationsCardPage cardPage = new IdeIntegrationsCardPage();
    new IdeIntegrationsCardPageAssertions(cardPage).shouldShowCardWithHeadingAndFooterLink();
  }

  @Test
  @Category(RegressionTest.class)
  public void testIdeCard_footerLinkTargetsIdeRoute() {
    IdeIntegrationsCardPage cardPage = new IdeIntegrationsCardPage();
    new IdeIntegrationsCardPageAssertions(cardPage).shouldHaveFooterLinkPointingToIdeRoute();
  }
}
