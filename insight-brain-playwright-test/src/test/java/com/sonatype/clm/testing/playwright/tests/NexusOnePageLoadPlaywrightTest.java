/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.NexusOnePage;
import com.sonatype.clm.testing.playwright.pages.NexusOnePageAssertions;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Verifies that the Nexus One SPA loads and renders shell routes after Epic 2.
 *
 * <p>
 * Ported from the Selenide {@code NexusOnePageLoadTest}.
 */
public class NexusOnePageLoadPlaywrightTest
    extends AbstractIqUiTest
{
  /**
   * Enable the master flag, then log in on the classic shell — the NexusOneIndexAccessFilter
   * redirects anonymous visitors to {@code /assets/index.html} regardless of the master flag,
   * so the SPA is only reachable by an authenticated session that visits the nexus-one URL
   * afterwards.
   */
  @Before
  public void enableNexusOneUiAndLogin() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    playwrightOpenAndWaitForVisible(LoginPage.rootUrl(), new LoginPage().modal());
    playwrightLogin();
  }

  /**
   * README §7b: every system-config / feature-flag toggle must be paired with an
   * {@code @After} that restores the default so the next test in the same fork starts from
   * a known state. Reset is only on {@code @After} (not also {@code @Before}) because the
   * {@code @Before} above flips the flag to {@code true} and JUnit 4 makes no ordering
   * guarantees between sibling {@code @Before} methods.
   */
  @After
  public void resetPreviewNexusOneUiFlag() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  @Test
  @Category(SanityTest.class)
  public void testNexusOneSpaLoads() {
    playwrightRefreshOrOpen(NexusOnePage.url("/home"));

    NexusOnePage page = new NexusOnePage();
    NexusOnePageAssertions assertions = new NexusOnePageAssertions(page);
    assertions.shouldBeVisible();
    assertions.shouldHaveHeadingText("Nexus One");
  }

  /**
   * Sibling of {@link #testNexusOneSpaLoads()}: confirms the SPA's hash router resolves
   * routes other than the default {@code /home}. Uses {@code /coming-soon/system-config} as a
   * representative non-home route since it exercises a distinct page component
   * ({@code ComingSoonPage}) with its own h1 ("Coming Soon") that we can assert on.
   */
  @Test
  @Category(SanityTest.class)
  public void testNexusOneSpaLoadsAtNonHomeRoute() {
    playwrightRefreshOrOpen(NexusOnePage.url("/coming-soon/system-config"));

    NexusOnePage page = new NexusOnePage();
    NexusOnePageAssertions assertions = new NexusOnePageAssertions(page);
    assertions.shouldBeVisible();
    assertions.shouldHaveHeadingText("Coming Soon");
  }
}
