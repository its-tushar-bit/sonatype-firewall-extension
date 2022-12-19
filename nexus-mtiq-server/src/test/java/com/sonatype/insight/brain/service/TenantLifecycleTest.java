/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.tenancy.MultiTenantTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setTenant;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantLifecycleTest
    extends MultiTenantTest
{
  @Mock
  CLMLicenseManager licenseManager;

  @Mock
  DataMigrator dataMigrator;

  @Mock
  NewInstancePopulator newInstancePopulator;

  @Mock
  InsightConfig config;

  @Mock
  OperationalDataStore operationalDataStore;

  @Mock
  AggregationDataStore aggregationDataStore;

  @Mock
  DataMartDataStore dataMartDataStore;

  @Mock
  ThirdPartyScansDataStore thirdPartyScansDataStore;

  @Mock
  DatabaseProvisionUtils databaseProvisionUtils;

  @Mock
  DatabaseConfigProvider databaseConfigProvider;

  TenantLifecycle underTest;

  @Before
  @Override
  public void setup() {
    underTest = new TestTenantLifecycle(licenseManager,
        dataMigrator,
        newInstancePopulator,
        config,
        operationalDataStore,
        aggregationDataStore,
        dataMartDataStore,
        thirdPartyScansDataStore,
        databaseProvisionUtils,
        databaseConfigProvider);
  }

  @Test
  public void shouldBootTenant() throws Exception {
    setTenant(new Tenant("tenant"));

    underTest.bootTenant();

    verify(databaseProvisionUtils).initializeDatabases(eq(config), any(DatabaseConfigProvider.class));

    verify(dataMigrator).migrate();
    verify(licenseManager).loadLicense();
    verify(newInstancePopulator).populateIfNewInstance();
  }

  @Test
  public void shouldAttemptToLoadLicenseFile_whenLicenseSpecified() throws Exception {
    setTenant(new Tenant("tenant"));

    String sonatypeWorkDir = "tenant/work";
    String licenseFile = "license.lic";

    when(config.getLicenseFile()).thenReturn(licenseFile);
    when(config.getSonatypeWork()).thenReturn(new File(sonatypeWorkDir));

    underTest.bootTenant();

    verify(licenseManager).installLicenseIfUnlicensed(sonatypeWorkDir + "/" + licenseFile);
  }

  private static class TestTenantLifecycle
      extends TenantLifecycle
  {
    DatabaseProvisionUtils databaseProvisionUtils;

    public TestTenantLifecycle(
        CLMLicenseManager licenseManager,
        DataMigrator dataMigrator,
        NewInstancePopulator newInstancePopulator,
        InsightConfig config,
        OperationalDataStore operationalDataStore,
        AggregationDataStore aggregationDataStore,
        DataMartDataStore dataMartDataStore,
        ThirdPartyScansDataStore thirdPartyScansDataStore,
        DatabaseProvisionUtils databaseProvisionUtils,
        DatabaseConfigProvider databaseConfigProvider)
    {
      super(licenseManager, dataMigrator, newInstancePopulator, config, operationalDataStore, aggregationDataStore,
          dataMartDataStore, thirdPartyScansDataStore, databaseConfigProvider);
      this.databaseProvisionUtils = databaseProvisionUtils;
    }

    @Override
    protected DatabaseProvisionUtils getDatabaseProvisionUtils() {
      return databaseProvisionUtils;
    }
  }
}
