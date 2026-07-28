/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.util.regex.Pattern;

import com.microsoft.playwright.Route;
import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.RoiFirewallMetricsPage;
import com.sonatype.clm.testing.playwright.pages.RoiFirewallMetricsPageAssertions;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * MTIQ — ROI Firewall Metrics tile on the Firewall Dashboard.
 *
 * <p>
 * Metric values (malware, namespace, safe-components) are hardcoded in
 * {@code roiFirewallMetricsSlice.js} — no backend ROI call is made. The only live network
 * call is the {@code CONFIGURE_SYSTEM} permission check that controls visibility of the
 * "Configure ROI values" link.
 *
 * <p>
 * MTIQ divergence: three Firewall dashboard endpoints fail in the embedded server, setting
 * {@code viewState.loadError} and hiding the tile. They are stubbed via {@code page.route()}.
 */
@Category(MtiqTest.class)
public class MtiqRoiFirewallMetricsPlaywrightTest
    extends AbstractMtiqUiTest
{
  private static final String FIREWALL_CONFIG_STUB = "[]";

  private static final String FIREWALL_RELEASE_QUARANTINE_SUMMARY_STUB =
      "{\"autoReleaseQuarantineCountMTD\":0,\"autoReleaseQuarantineCountYTD\":0}";

  private static final String FIREWALL_QUARANTINE_SUMMARY_STUB =
      "{\"quarantineEnabled\":false,\"quarantineEnabledRepositoryCount\":0," +
          "\"repositoryCount\":0,\"totalComponentCount\":0,\"quarantinedComponentCount\":0}";

  private static final Pattern FIREWALL_CONFIG_PATTERN =
      Pattern.compile(".*/api/v2/firewall/releaseQuarantine/configuration([?#].*)?$");

  private static final Pattern FIREWALL_RQ_SUMMARY_PATTERN =
      Pattern.compile(".*/api/v2/firewall/releaseQuarantine/summary([?#].*)?$");

  private static final Pattern FIREWALL_QUARANTINE_SUMMARY_PATTERN =
      Pattern.compile(".*/api/v2/firewall/quarantine/summary([?#].*)?$");

  private RoiFirewallMetricsPage roiFirewallMetricsPage;

  private RoiFirewallMetricsPageAssertions roiFirewallMetricsAssertions;

  @Before
  public void setUp() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    roiFirewallMetricsPage = new RoiFirewallMetricsPage();
    roiFirewallMetricsAssertions = new RoiFirewallMetricsPageAssertions(roiFirewallMetricsPage);
    stubFirewallDashboardEndpoints();
  }

  @After
  public void tearDown() {
    page.unrouteAll();
  }

  @Test
  public void testRoiFirewallMetrics_tileRendersWithThreeMetricsAndConfigureLink() {
    playwrightLoginAdminAt(RoiFirewallMetricsPage.url());

    roiFirewallMetricsAssertions.shouldShowTileWithThreeMetrics();
    roiFirewallMetricsAssertions.shouldShowConfigureRoiLink();
  }

  @Test
  public void testRoiFirewallMetrics_configureRoiLinkHiddenForNonAdmin() {
    // newUser() creates a user with no global permissions — no CONFIGURE_SYSTEM — so the link is hidden.
    User user = newUser();
    playwrightLoginAt(RoiFirewallMetricsPage.url(), user.getUsername(), user.getPassword());

    roiFirewallMetricsAssertions.shouldShowTileWithThreeMetrics();
    assertThat(roiFirewallMetricsPage.configureLink()).isHidden();
  }

  // MTIQ exception: Firewall index unavailable in embedded test server — stub these endpoints
  // so LoadWrapper does not hide FirewallTabs (and RoiFirewallMetrics) on load failure.
  private void stubFirewallDashboardEndpoints() {
    page.route(FIREWALL_CONFIG_PATTERN,
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody(FIREWALL_CONFIG_STUB)));

    page.route(FIREWALL_RQ_SUMMARY_PATTERN,
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody(FIREWALL_RELEASE_QUARANTINE_SUMMARY_STUB)));

    page.route(FIREWALL_QUARANTINE_SUMMARY_PATTERN,
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody(FIREWALL_QUARANTINE_SUMMARY_STUB)));
  }
}
