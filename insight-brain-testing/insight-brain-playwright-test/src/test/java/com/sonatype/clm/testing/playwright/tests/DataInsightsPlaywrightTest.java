/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.microsoft.playwright.Route;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.DataInsightsPage;
import com.sonatype.clm.testing.playwright.pages.DataInsightsPageAssertions;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Loading-error retry is deliberately not covered here — driving that branch requires
 * intercepting the underlying Looker iframe embed initialisation and forcing it into a rejected
 * state, which is outside the useful scope of a UI smoke check. See the batch divergences log.
 */
public class DataInsightsPlaywrightTest
    extends AbstractIqUiTest
{
  @Before
  public void enableFeatureAndOpenLoggedIn() {
    enableIntegratedEnterpriseReportingOnLicense();
    playwrightRefreshOrOpen(DataInsightsPage.url());
    playwrightLogin();
  }

  @After
  public void unrouteAll() {
    // The license-gate test registers a page.route intercept on the shared BrowserContext; clear
    // it so subsequent tests in the same fork don't inherit the stub. The setFeatures() mutation
    // from @Before is auto-reset by AbstractIqUiTest#initMocks
    // (productLicenseManager.wasChanged() → reset+installLicense).
    page.unrouteAll();
  }

  private void enableIntegratedEnterpriseReportingOnLicense() {
    Set<LicensedFeature> baseline = productLicenseManager.getFeatures();
    EnumSet<LicensedFeature> merged = baseline == null || baseline.isEmpty()
        ? EnumSet.noneOf(LicensedFeature.class)
        : EnumSet.copyOf(baseline);
    merged.add(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);
    setFeatures(merged.toArray(new LicensedFeature[0]));
  }

  /**
   * The inner {@code #labs-container} div is only rendered on a successful Looker iframe load —
   * under the embedded test server the Looker endpoint 404s, so we scope the assertion to the
   * outer container only.
   */
  @Test
  @Category(RegressionTest.class)
  public void testDataInsights_pageRendersWithOuterContainerWhenFeatureEnabled() {
    new DataInsightsPageAssertions(new DataInsightsPage()).shouldShowContainer();
  }

  /**
   * With {@code integrated-enterprise-reporting} absent from the product-features response, the
   * license-error copy renders inside the main container. Intercepts {@code /rest/product/features}
   * to return an empty features array.
   */
  @Test
  @Category(RegressionTest.class)
  public void testDataInsights_licenseGateShowsErrorMessageWhenReportingUnsupported() {
    page.route(Pattern.compile(".*/rest/product/features([?#][^/]*)?$"),
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody("[]")));

    // Reload so fetchProductFeaturesIfNeeded re-fires through the intercept and Redux state
    // refetches the (now empty) product-features response.
    page.reload();

    new DataInsightsPageAssertions(new DataInsightsPage()).shouldShowLicenseGateError();
  }
}
