/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Nexus One SPA served at {@code /assets/nexus-one/index.html}.
 *
 * <p>
 * The Selenide predecessor anchored to the CSS selector
 * {@code #nexus-one-root [data-testid='nexus-one-page-surface']}; following the README
 * authoring rules we rely on the React Testing Library priority order
 * (role &gt; label &gt; placeholder &gt; text &gt; testid &gt; CSS), so locators here use
 * {@link #byRole}/{@code data-testid} instead of raw CSS selectors. The
 * {@code nexus-one-page-surface} test id is kept solely as a stable handle for the
 * theme-coloured Radix {@code <Theme>} wrapper that we read computed styles from in
 * {@code NexusOneThemePlaywrightTest} — it is a non-interactive {@code <div>} with no
 * meaningful ARIA role, so role-based location is not viable for that one element.
 */
public class NexusOnePage
    extends BasePage
{
  /**
   * Server-relative URL of the Nexus One SPA index page, with no hash route. Returns
   * {@code "/assets/nexus-one/index.html"}; Playwright tests resolve this against the server
   * base URL pre-configured on the {@code BrowserContext}.
   */
  public static String url() {
    return "/assets/nexus-one/index.html";
  }

  /**
   * Same as {@link #url()} but suffixed with {@code #} + {@code hashRoute} so the SPA's hash
   * router lands on a specific route on load. Mirrors the Selenide predecessor's
   * {@code BaseUrl.rootUriBuilder().path("assets/nexus-one/index.html").fragment(hashRoute)}.
   */
  public static String url(String hashRoute) {
    return url() + "#" + hashRoute;
  }

  public NexusOnePage() {
    super();
  }

  /**
   * Radix {@code <Theme>} wrapper around the Nexus One page contents, marked with
   * {@code data-testid="nexus-one-page-surface"}. Used as the colour-bearing surface in
   * theme-rendering tests; its computed {@code background-color} reflects the active
   * (light / dark) Radix theme appearance.
   */
  public Locator pageSurface() {
    return byTestId("nexus-one-page-surface");
  }

  /**
   * The single page-level h1 heading rendered inside the {@link #pageSurface()}.
   * <p>
   * On the Platform Home route this reads {@code "Nexus One"}; on a Coming Soon route it
   * reads {@code "Coming Soon"}. Locating by ARIA role+level rather than CSS keeps the test
   * coupled to the page's accessibility contract, not to its DOM tag soup.
   */
  public Locator heading() {
    return pageSurface().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }
}
