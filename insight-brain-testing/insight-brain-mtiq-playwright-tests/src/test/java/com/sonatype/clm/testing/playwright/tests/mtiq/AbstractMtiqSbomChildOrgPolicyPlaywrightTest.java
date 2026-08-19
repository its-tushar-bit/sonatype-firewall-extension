/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;

/**
 * Single-@Test-per-class isolation for child-org policy scenarios — MTIQ SPA URL guard rejects
 * consecutive child-org navigations after a prior policy-editor visit.
 * TODO(CLM-42839): consolidate once the SPA URL-guard bug is fixed upstream.
 */
abstract class AbstractMtiqSbomChildOrgPolicyPlaywrightTest
    extends AbstractMtiqUiTest
{
  protected static final int THREAT_LEVEL = MtiqSbomPolicyTestConstants.THREAT_LEVEL;

  protected Organization rootOrg;

  protected Organization childOrg;

  @BeforeEach
  public final void seedTreeAndLogin() {
    rootOrg = tempEntity.newOrganization("MTIQ Policy Root " + tempEntity.uuid());
    childOrg = tempEntity.newOrganization("MTIQ Policy Child " + tempEntity.uuid(), rootOrg);

    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_FOUNDATION);
    playwrightRefreshOrOpen("/");
    playwrightLogin();
  }
}
