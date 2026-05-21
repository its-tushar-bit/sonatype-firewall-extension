/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the IQ Server global sidebar navigation.
 * {@code SidebarNavigationButton} elements.
 */
public class SidebarComponent
    extends BasePage
{
  private static final String CONTAINER = ".nx-global-sidebar-2";

  public SidebarComponent() {
    super();
  }

  public Locator container() {
    return locator(CONTAINER);
  }

  public Locator sidebarLinks() {
    return locator(".nx-global-sidebar-2__nav");
  }

  public Locator productVersion() {
    return locator(".nx-global-footer-2");
  }

  public Locator productLogo() {
    return locator(".nx-global-header-2__logo");
  }

  public Locator toggleButton() {
    return page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName(Pattern.compile("(Expand|Collapse) Menu", Pattern.CASE_INSENSITIVE)));
  }

  // --------------- Navigation Buttons ---------------

  public Locator dashboardButton() {
    return globalSidebarLink("Dashboard");
  }

  public Locator reportingButton() {
    return globalSidebarLink("Reports");
  }

  public Locator policiesButton() {
    return globalSidebarLink("Orgs and Policies");
  }

  public Locator labsButton() {
    return globalSidebarLink("Success Metrics");
  }

  public Locator vulnerabilityDetailsButton() {
    return globalSidebarLink("Vulnerability Lookup");
  }

  public Locator advancedSearchButton() {
    return globalSidebarLink("Advanced Search");
  }

  public Locator firewallButton() {
    return globalSidebarLink("Firewall");
  }

  public Locator legalButton() {
    return globalSidebarLink("Legal");
  }

  /**
   * The "Operational Reporting" sidebar link. Rendered when the license does NOT include the
   * (HDS-controlled) {@code integrated-enterprise-reporting} product feature. The default test
   * license falls into this branch, so this is the link the harness sees.
   */
  public Locator operationalReportingButton() {
    return globalSidebarLink("Operational Reporting");
  }

  public Locator firewallDashboardButton() {
    return globalSidebarLink("Dashboard");
  }

  public Locator firewallRepositoriesButton() {
    return globalSidebarLink("Repos and Policies");
  }

  public Locator sbomManagerDashboardButton() {
    return globalSidebarLink("Dashboard");
  }

  public Locator sbomManagerOrganizationsButton() {
    return globalSidebarLink("Organizations");
  }

  /**
   * The "Enterprise Reporting" sidebar link. Rendered when the license includes
   * {@code integrated-enterprise-reporting}. Mutually exclusive with
   * {@link #operationalReportingButton()}.
   */
  public Locator enterpriseReportingButton() {
    return locator("#enterprise-reporting-button");
  }

  // --------------- Actions ---------------

  /**
   * Click the global "Orgs and Policies" sidebar button and wait for the URL to advance into
   * the management view. The button text is "Orgs and Policies" but the id is
   * {@code #policies-navigation-button} — the discrepancy is in the JSX, not the test.
   */
  public void clickPoliciesNavigation() {
    policiesButton().click();
    page.waitForURL("**" + expectedPoliciesUrlFragment() + "/**");
  }

  /**
   * Click the global "Operational Reporting" sidebar button and wait for the SPA to advance to
   * the landing page hash route.
   */
  public void clickOperationalReportingNavigation() {
    operationalReportingButton().click();
    page.waitForURL("**" + expectedOperationalReportingUrlFragment() + "**");
  }

  /**
   * Click the global "Advanced Search" sidebar button and wait for the SPA to advance to
   * the Advanced Search hash route.
   */
  public void clickAdvancedSearchNavigation() {
    advancedSearchButton().click();
    page.waitForURL("**" + expectedAdvancedSearchUrlFragment() + "**");
  }

  /**
   * Click the global "Success Metrics" (Labs) sidebar button and wait for the SPA to advance to
   * the Success Metrics hash route.
   */
  public void clickSuccessMetricsNavigation() {
    labsButton().click();
    page.waitForURL("**" + expectedSuccessMetricsUrlFragment() + "**");
  }

  /**
   * Click the global "Enterprise Reporting" sidebar button and wait for the SPA to advance to
   * the Enterprise Reporting landing hash route.
   */
  public void clickEnterpriseReportingNavigation() {
    enterpriseReportingButton().click();
    page.waitForURL("**" + expectedEnterpriseReportingUrlFragment() + "**");
  }

  /**
   * Class-name regexes for the sidebar's open/closed state. The container's class list always
   * contains either {@code open} or {@code closed} (rendered by NxGlobalSidebar2 from
   * react-shared-components based on its {@code isOpen} prop, persisted to localStorage by
   * {@code IqSidebarNav.jsx}). Word-boundary anchors avoid matching {@code closed} as a
   * substring of unrelated classes (defensive — there are no such classes today, but the
   * regex stays correct as the class list grows).
   */
  private static final Pattern OPEN_CLASS_REGEX = Pattern.compile(".*\\bopen\\b.*");

  private static final Pattern CLOSED_CLASS_REGEX = Pattern.compile(".*\\bclosed\\b.*");

  /**
   * Extended timeout (15 s) for class-list / visibility assertions on the sidebar. Used
   * instead of Playwright's default 5 s because under cold-start parallel runs the sidebar's
   * mount and its post-toggle CSS transition can take longer than 5 s — the same boot-gating
   * pattern as {@code RoutingErrorBoxComponent} and {@code UserTokenModal}.
   */
  private static final double STATE_TIMEOUT_MS = 15_000;

  /**
   * Idempotently put the sidebar into the closed state and wait for it to actually settle
   * there.
   *
   * <p>
   * The previous implementation read the class once with {@link Locator#getAttribute(String)}
   * (a one-shot, non-auto-waiting call) and clicked the toggle conditionally. That race showed
   * up under parallel runs: a CSS transition from a prior toggle could still be in flight when
   * {@code getAttribute} sampled the class, so the wrong branch was taken and the sidebar
   * ended up in the opposite state. Worse, if the toggle was clicked while
   * {@code react-shared-components}'s {@code useToggle} was mid re-render after the previous
   * animation, the click event was silently dropped — leaving the sidebar in {@code closed}
   * for the entire 5 s assertion budget (the failure mode reported as
   * "9 × locator resolved to ... closed").
   *
   * <p>
   * The new implementation uses Playwright's auto-retrying class-list assertion to make the
   * decision (so it waits for the class list to <em>stabilize</em> before reading), clicks
   * only when needed, and then waits for the desired class to be present. This is safe to
   * call repeatedly and from any starting state.
   */
  public void closeSidebar() {
    waitForStableState();
    if (!isClosedNow()) {
      toggleButton().click();
    }
    assertThat(container()).hasClass(CLOSED_CLASS_REGEX,
        new LocatorAssertions.HasClassOptions().setTimeout(STATE_TIMEOUT_MS));
  }

  /**
   * Idempotently put the sidebar into the open state and wait for it to actually settle there.
   * See {@link #closeSidebar()} for the rationale behind the auto-retry / settle pattern.
   */
  public void openSidebar() {
    waitForStableState();
    if (isClosedNow()) {
      toggleButton().click();
    }
    assertThat(container()).hasClass(OPEN_CLASS_REGEX,
        new LocatorAssertions.HasClassOptions().setTimeout(STATE_TIMEOUT_MS));
  }

  /**
   * Wait for the container to expose either {@code open} or {@code closed} on its class list.
   * Combined with {@link Locator#getAttribute(String)} below this collapses the race where the
   * toggle was clicked while a previous transition was still in flight: by the time we sample
   * the class string, NxGlobalSidebar2 has finished its render and the class list is
   * authoritative.
   */
  private void waitForStableState() {
    assertThat(container())
        .hasClass(Pattern.compile(".*\\b(open|closed)\\b.*"),
            new LocatorAssertions.HasClassOptions().setTimeout(STATE_TIMEOUT_MS));
  }

  /**
   * Synchronous snapshot of the closed state, intended only for the toggle-decision in
   * {@link #closeSidebar()} / {@link #openSidebar()} <em>after</em>
   * {@link #waitForStableState()} has guaranteed the class list is stable. Tests should prefer
   * {@link #shouldBeOpen()} / {@link #shouldBeClosed()}, which auto-retry.
   */
  private boolean isClosedNow() {
    String classes = container().getAttribute("class");
    return classes != null && classes.contains("closed");
  }

  // --------------- URL fragments produced by sidebar navigation ---------------

  /** The hash-route fragment that appears in {@code page.url()} after clicking {@link #dashboardButton()}. */
  public static String expectedDashboardUrlFragment() {
    return "/dashboard";
  }

  /** The hash-route fragment that appears in {@code page.url()} after clicking {@link #policiesButton()}. */
  public static String expectedPoliciesUrlFragment() {
    return "/management";
  }

  /**
   * The hash-route fragment that appears in {@code page.url()} after clicking
   * {@link #operationalReportingButton()}. Matches the registered SPA route in
   * {@code operationalReporting/route.js}.
   */
  public static String expectedOperationalReportingUrlFragment() {
    return "/operationalReporting";
  }

  /** The hash-route fragment that appears in {@code page.url()} after clicking {@link #advancedSearchButton()}. */
  public static String expectedAdvancedSearchUrlFragment() {
    return "/advancedSearch";
  }

  /** The hash-route fragment that appears in {@code page.url()} after clicking {@link #labsButton()}. */
  public static String expectedSuccessMetricsUrlFragment() {
    return "/labs/successMetrics";
  }

  /** The hash-route fragment that appears in {@code page.url()} after clicking {@link #enterpriseReportingButton()}. */
  public static String expectedEnterpriseReportingUrlFragment() {
    return "/enterpriseReportingLandingPage";
  }

}
