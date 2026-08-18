/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.Set;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the AI Developer opt-in in MTIQ, where the property that unlocks the feature lives in each tenant's own
 * schema. The license gate, the property store and the server configuration are all real here; only the licensed
 * product set is stubbed, to a Lifecycle license that does not carry the AI Developer entitlement.
 */
public class MultiTenantAiDeveloperOptInTest
    extends AbstractMultiTenantDatabaseTest
{
  private static final String OPT_IN = "admin,2026-08-17T09:15:00Z";

  private SystemConfigurationPropertyDAO configurationPropertyDAO;

  private DefaultProductLicense license;

  @BeforeEach
  public void before() {
    // The property cache is static and keyed by data store, so drop it before reading in a freshly provisioned tenant.
    SystemConfigurationPropertyDAO.invalidateEntireCache();
    configurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    license = licenseWithProducts(configurationPropertyDAO, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
  }

  /**
   * The asserted product is the AiDeveloperSaas SKU rather than the self-hosted one, because that is the id
   * {@code SolutionResolver} consults in a multi-tenant deployment to decide whether the product switcher offers AI
   * Developer.
   */
  @Test
  public void optInGrantsAiDeveloperToTheTenantThatRecordedItAndToNoOther() {
    testAsNewTenant(optedIn -> {
      configurationPropertyDAO.set(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN, OPT_IN);

      assertThat(license.hasFeature(LicensedFeature.AI_DEVELOPER)).isTrue();
      assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)).isTrue();
    });

    testAsNewTenant(other -> {
      assertThat(configurationPropertyDAO.get(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN)).isNull();
      assertThat(license.hasFeature(LicensedFeature.AI_DEVELOPER)).isFalse();
      assertThat(license.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)).isFalse();
    });
  }

  /**
   * Property reads fall back to the global tenant for any name a tenant has not set, so a value recorded there unlocks
   * AI Developer for every tenant that has not opted in on its own. Only a Sonatype operator can write the global
   * tenant, which makes this a deliberate lever rather than a path between tenants.
   */
  @Test
  public void optInRecordedOnTheGlobalTenantReachesTenantsWithoutTheirOwn() {
    recordGlobalOptIn(OPT_IN);
    try {
      testAsNewTenant(inheriting -> assertThat(license.hasFeature(LicensedFeature.AI_DEVELOPER)).isTrue());
    }
    finally {
      recordGlobalOptIn(null);
    }
  }

  /**
   * A tenant writing its own opt-in must land in the tenant's schema even when the global tenant already has a value,
   * because the write resolves the existing row through that same global fallback.
   */
  @Test
  public void aTenantRecordsItsOwnOptInOverTheGlobalOne() {
    String tenantOptIn = "tenant-admin,2026-08-17T10:30:00Z";

    recordGlobalOptIn(OPT_IN);
    try {
      testAsNewTenant(own -> {
        configurationPropertyDAO.set(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN, tenantOptIn);

        assertThat(configurationPropertyDAO.get(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN))
            .isEqualTo(tenantOptIn);
        assertThat(license.hasFeature(LicensedFeature.AI_DEVELOPER)).isTrue();
      });

      testAsGlobalTenant(global -> assertThat(
          configurationPropertyDAO.get(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN)).isEqualTo(OPT_IN));
    }
    finally {
      recordGlobalOptIn(null);
    }
  }

  /**
   * An opt-in row outlives the license that gave it meaning: a tenant can lose its license, or be deleted before the
   * row is. The gate requires a Lifecycle product, so a tenant holding no licensed product gains nothing from a row
   * that is still there.
   */
  @Test
  public void optInGrantsNothingToATenantHoldingNoLicensedProduct() {
    DefaultProductLicense unlicensed = licenseWithProducts(configurationPropertyDAO);

    testAsNewTenant(stale -> {
      configurationPropertyDAO.set(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN, OPT_IN);

      assertThat(unlicensed.hasFeature(LicensedFeature.AI_DEVELOPER)).isFalse();
      assertThat(unlicensed.hasProduct(ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS)).isFalse();
    });
  }

  // The global tenant's schema outlives each test and every tenant read falls back to it, so a value written there has
  // to be removed again or it grants AI Developer to the tenants of whichever test runs next.
  private void recordGlobalOptIn(String value) {
    testAsGlobalTenant(global -> configurationPropertyDAO.set(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN, value));
  }

  private static DefaultProductLicense licenseWithProducts(SystemConfigurationPropertyDAO dao, String... products) {
    // MultiTenantInsightConfig always reports an external database, so the opt-in is never refused for that reason.
    return new DefaultProductLicense(Mockito.mock(DeveloperEnablementService.class), dao,
        new MultiTenantInsightConfig())
    {
      @Override
      public Set<String> getProducts() {
        return Set.of(products);
      }

      @Override
      public Set<LicensedFeature> getFeatures() {
        return Set.of();
      }
    };
  }
}
