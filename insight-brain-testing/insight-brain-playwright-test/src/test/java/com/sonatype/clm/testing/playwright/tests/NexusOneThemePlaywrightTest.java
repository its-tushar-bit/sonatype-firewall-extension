/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ColorScheme;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.NexusOnePage;
import com.sonatype.clm.testing.playwright.pages.NexusOnePageAssertions;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the Nexus One SPA correctly reacts to theme settings, including live OS
 * color-scheme changes via {@code prefers-color-scheme}.
 *
 * <p>
 * Rather than testing CSS class names, these tests verify actual rendered colors: a dark
 * theme should have a dark background ({@code &lt; 50%} brightness) with light text, and a
 * light theme should have a light background ({@code &gt; 50%} brightness) with dark text.
 * The brightness-band assertion lives on {@link NexusOnePageAssertions} per the module
 * authoring rules — see {@code shouldHaveLightAppearance()} / {@code shouldHaveDarkAppearance()}.
 *
 * <p>
 * System color-scheme changes are emulated via Playwright's native
 * {@link Page#emulateMedia(Page.EmulateMediaOptions)}, which sets {@code prefers-color-scheme}
 * on the current {@link Page} (no CDP HTTP plumbing required, unlike the Selenide
 * predecessor).
 *
 * <p>
 * Categorised as {@link RegressionTest} (not {@link SanityTest} like the sibling page-load
 * and redirect tests) because the OS-emulation matrix is slower than a plain page-load check
 * and the preview-only feature flag is not yet on the merge-blocking critical path.
 *
 * <p>
 * Ported from the Selenide {@code NexusOneThemeTest}.
 */
public class NexusOneThemePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String MODE_DARK = "dark";

  private static final String MODE_LIGHT = "light";

  @BeforeEach
  public void enableNexusOneUiAndLogin() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    // Log in on the classic shell first so the NexusOneIndexAccessFilter will allow the
    // subsequent SPA navigation (the filter rejects anonymous callers regardless of the flag).
    playwrightOpenAndWaitForVisible(LoginPage.rootUrl(), new LoginPage().modal());
    playwrightLogin();
  }

  /**
   * Reset the feature flag so the next test in the same fork starts from a known state. The
   * flag-reset must run unconditionally — if {@code enableNexusOneUiAndLogin} fails after
   * flipping the flag (e.g. the login times out), the leaked flag would taint everything that
   * runs next in the same fork.
   *
   * <p>
   * No need to clear {@code localStorage['nosc.themeMode']} or reset {@code emulateMedia} here:
   * {@code AbstractPlaywrightTest} provides a per-test {@link com.microsoft.playwright.BrowserContext}
   * that is torn down after every test, so theme override and prefers-color-scheme emulation
   * cannot leak across tests. Touching {@code page} here would also be unsafe if
   * {@code enableNexusOneUiAndLogin} failed before any navigation (it would throw
   * {@code PlaywrightException} on an opaque {@code about:blank} origin and mask the original
   * failure cause).
   */
  @AfterEach
  public void resetPreviewNexusOneUiFlag() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  @Test
  @Tag("regression")
  public void testSystemLightModeProducesLightAppearance() {
    page.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(ColorScheme.LIGHT));
    NexusOnePageAssertions assertions = openNexusOnePage();

    assertions.shouldHaveLightAppearance();
  }

  @Test
  @Tag("regression")
  public void testSystemDarkModeProducesDarkAppearance() {
    page.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(ColorScheme.DARK));
    NexusOnePageAssertions assertions = openNexusOnePage();

    assertions.shouldHaveDarkAppearance();
  }

  @Test
  @Tag("regression")
  public void testThemeUpdatesReactivelyWhenSystemColorSchemeChanges() {
    page.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(ColorScheme.LIGHT));
    NexusOnePageAssertions assertions = openNexusOnePage();
    assertions.shouldHaveLightAppearance();

    // Switch OS to dark mode without reloading the page.
    page.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(ColorScheme.DARK));

    assertions.shouldHaveDarkAppearance();
  }

  @Test
  @Tag("regression")
  public void testExplicitDarkThemeOverridesSystemPreference() {
    page.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(ColorScheme.LIGHT));
    NexusOnePageAssertions assertions = openNexusOnePage();
    assertions.shouldHaveLightAppearance();

    setThemeModeViaLocalStorage(MODE_DARK);

    assertions.shouldHaveDarkAppearance();
  }

  @Test
  @Tag("regression")
  public void testExplicitLightThemeOverridesSystemPreference() {
    page.emulateMedia(new Page.EmulateMediaOptions().setColorScheme(ColorScheme.DARK));
    NexusOnePageAssertions assertions = openNexusOnePage();
    assertions.shouldHaveDarkAppearance();

    setThemeModeViaLocalStorage(MODE_LIGHT);

    assertions.shouldHaveLightAppearance();
  }

  // ---- helpers ----

  private NexusOnePageAssertions openNexusOnePage() {
    playwrightRefreshOrOpen(NexusOnePage.url("/home"));
    NexusOnePageAssertions assertions = new NexusOnePageAssertions(new NexusOnePage());
    assertions.shouldBeVisible();
    return assertions;
  }

  /**
   * Set the Nexus One explicit-theme override in {@code localStorage} and dispatch the same-tab
   * event that {@code useNoscTheme} listens for. Mirrors the Selenide helper of the same name.
   * Callers should pass {@link #MODE_DARK} or {@link #MODE_LIGHT}; the parameter is typed as
   * {@code String} only because the value is forwarded straight to in-page JavaScript.
   */
  private void setThemeModeViaLocalStorage(String mode) {
    page.evaluate(
        """
            (mode) => {
              localStorage.setItem('nosc.themeMode', mode);
              window.dispatchEvent(new CustomEvent('nosc.themeMode.change', {
                detail: { themeMode: mode }
              }));
            }
            """,
        mode);
  }
}
