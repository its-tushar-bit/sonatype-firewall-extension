/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqFipsUiTest;
import com.sonatype.clm.testing.playwright.pages.MtiqUserManagementPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.TenantMetadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * User Management page under FIPS.
 *
 * <p>
 * {@code USER_MANAGEMENT_PAGES} {@code isEnabled()} returns
 * {@code isSingleTenant() || FIPSModeDetector.isEnabled()}. Under FIPS the flag is {@code true},
 * so {@code checkMtiqDefaultFlow} returns {@code false} — standard Users page renders instead
 * of the managed-IdP invite flow, even when {@code SSO_IDP_MANAGED_BY_SONATYPE} is seeded.
 */
@Tag("mtiq")
public class MtiqUserManagementFIPSPlaywrightTest
    extends AbstractMtiqFipsUiTest
{
  // TenantMetadata insert requires non-null values; the SPA only checks for row presence.
  private static final String TEST_APP_ID = "appId";

  private static final String TEST_APP_NAME = "appName";

  private static final String TEST_CONNECTION_ID = "connectionId";

  private static final String TEST_CONNECTION_NAME = "connectionName";

  private static final String TEST_ENCRYPTION_KEY_NAME = "encKeyName";

  // FIPS crypto overhead can delay the user-data round-trip beyond ELEMENT_TIMEOUT_MS.
  private static final LocatorAssertions.IsVisibleOptions SLOW_VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS);

  private MtiqUserManagementPage umPage;

  @BeforeEach
  public void seedManagedIdpAndCreatePageObjects() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE, String.valueOf(true));
    lookup(TenantMetadataDAO.class).insert(new TenantMetadata(
        TEST_APP_ID, TEST_APP_NAME, TEST_CONNECTION_ID,
        TEST_CONNECTION_NAME, TEST_ENCRYPTION_KEY_NAME, "", ""));
    umPage = new MtiqUserManagementPage();
  }

  @AfterEach
  public void cleanupTenantMetadata() {
    TenantMetadataDAO dao = lookup(TenantMetadataDAO.class);
    dao.getAll().forEach(dao::delete);
  }

  @Test
  public void testMtiqUserManagementFips_pageLoadsWithoutCryptoErrors() {
    playwrightLoginAdminAt(MtiqUserManagementPage.url());
    assertThat(umPage.pageTitle()).isVisible(SLOW_VISIBLE_OPTS);
  }

  @Test
  public void testMtiqUserManagementFips_inviteFlowAbsentUnderFips() {
    playwrightLoginAdminAt(MtiqUserManagementPage.url());
    assertThat(umPage.pageTitle()).isVisible(SLOW_VISIBLE_OPTS);
    assertThat(umPage.inviteUserButton()).isHidden(
        new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
  }
}
