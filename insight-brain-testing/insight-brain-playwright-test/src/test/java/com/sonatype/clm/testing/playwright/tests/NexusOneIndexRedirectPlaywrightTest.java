/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.microsoft.playwright.Page;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.LoginPageAssertions;
import com.sonatype.clm.testing.playwright.pages.NexusOnePage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link com.sonatype.insight.brain.landing.NexusOneIndexAccessFilter} redirects
 * to the classic shell when the master flag is OFF or the caller is anonymous.
 *
 * <p>
 * Ported from the Selenide {@code NexusOneIndexRedirectTest}.
 */
public class NexusOneIndexRedirectPlaywrightTest
    extends AbstractIqUiTest
{
  /**
   * The classic-shell SPA may append a hash route immediately after load, so the post-redirect
   * URL is matched as a substring rather than verbatim. Mirrors the deleted Selenide test's
   * pair of assertions: {@code urlContaining("/assets/index.html") && urlNotContaining("nexus-one")}.
   */
  private static final Pattern CLASSIC_INDEX_URL_PATTERN = Pattern.compile(".*/assets/index\\.html.*");

  /**
   * The master flag defaults to {@code false} and these tests verify behaviour with it off, so we
   * only need to reset on {@code @After} (not also {@code @Before}). Matches the doctrine in
   * {@code NexusOnePageLoadPlaywrightTest.resetPreviewNexusOneUiFlag} — sibling {@code @Before}
   * methods have no ordering guarantee in JUnit 4, so pairing a flag-flip with a stacked
   * {@code @Before @After} reset would silently race a future contributor's flag-enabling
   * {@code @Before}.
   */
  @AfterEach
  public void resetPreviewNexusOneUiFlag() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  /**
   * Defence-in-depth in case a prior test in the same fork failed to reset the flag. Cheap, idempotent.
   */
  @BeforeEach
  public void disablePreviewNexusOneUiBeforeTest() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  @Test
  @Tag("sanity")
  public void testAnonymousNexusOneIndexRedirectsToClassicShell() {
    playwrightRefreshOrOpen(NexusOnePage.url());

    waitUntilClassicIndexShell();
    new LoginPageAssertions(new LoginPage()).shouldBeVisible();
  }

  @Test
  @Tag("sanity")
  public void testAuthenticatedNexusOneIndexRedirectsWhenFlagOff() {
    playwrightOpenAndWaitForVisible(LoginPage.rootUrl(), new LoginPage().modal());
    playwrightLogin();

    playwrightRefreshOrOpen(NexusOnePage.url());

    waitUntilClassicIndexShell();
    // No additional surface-absent assertion on the Nexus One page object here: the
    // settled-URL assertions inside waitUntilClassicIndexShell() are the real signal. A
    // surface-absent check would also pass on about:blank or any unrelated route, so it would
    // give a false sense of additional coverage.
  }

  /**
   * Wait for the server filter (or client gate) to land us on the classic index, then snapshot
   * the settled URL once and assert both shape conditions synchronously. Reading the URL once
   * (rather than asserting with the auto-retrying {@code PlaywrightAssertions.assertThat(page)
   * .not().hasURL(...)}) preserves the strict pair-of-checks contract of the Selenide
   * predecessor: a hypothetical URL like {@code /assets/index.html#nexus-one-foo} would satisfy
   * {@code waitForURL} on the first pattern and (theoretically) sneak past a retrying negative
   * assertion if the SPA later scrubs the fragment. Splitting the two checks (rather than using
   * a single negative-lookahead regex) keeps the assertion pair grep-able and lets the two
   * conditions fail with distinct, meaningful messages.
   */
  private void waitUntilClassicIndexShell() {
    page.waitForURL(CLASSIC_INDEX_URL_PATTERN,
        new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_SUBSTRING_TIMEOUT_MS));
    String settledUrl = page.url();
    assertThat(settledUrl).contains("/assets/index.html");
    assertThat(settledUrl).doesNotContain("nexus-one");
  }
}
