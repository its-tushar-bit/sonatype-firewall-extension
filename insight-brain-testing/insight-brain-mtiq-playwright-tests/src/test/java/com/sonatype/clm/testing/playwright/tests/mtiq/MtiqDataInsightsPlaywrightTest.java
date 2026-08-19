/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.DataInsightsPage;
import com.sonatype.clm.testing.playwright.pages.DataInsightsPageAssertions;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * MTIQ — Data Insights page: feature-gated container visibility.
 *
 * <p>
 * MTIQ divergence: a fresh tenant has no licensed features by default, so
 * {@code INTEGRATED_ENTERPRISE_REPORTING} is absent without any {@code page.route()} intercept
 * or reload. On-prem tests intercept {@code /rest/product/features} and reload to simulate the
 * same condition.
 *
 * <p>
 * The inner {@code #labs-container} (Looker iframe host) is not asserted — the Looker
 * endpoint 404s under the embedded test server.
 */
@Tag("mtiq")
public class MtiqDataInsightsPlaywrightTest
    extends AbstractMtiqUiTest
{
  private DataInsightsPage dataInsightsPage;

  private DataInsightsPageAssertions dataInsightsAssertions;

  @BeforeEach
  public void setUp() {
    dataInsightsPage = new DataInsightsPage();
    dataInsightsAssertions = new DataInsightsPageAssertions(dataInsightsPage);
  }

  @Test
  public void testDataInsights_pageRendersWithOuterContainerWhenFeatureEnabled() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    setFeatures(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);
    playwrightLoginAdminAt(DataInsightsPage.url());

    dataInsightsAssertions.shouldShowContainer();
  }

  @Test
  public void testDataInsights_licenseGateShowsErrorMessageWhenReportingUnsupported() {
    // INTEGRATED_ENTERPRISE_REPORTING intentionally absent — a fresh MTIQ tenant has no features
    // by default, so the real server returns an empty product-features response and the license
    // gate renders immediately without a page.route() intercept or page.reload().
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(DataInsightsPage.url());

    dataInsightsAssertions.shouldShowLicenseGateError();
  }
}
