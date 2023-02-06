/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.tenancy.MultiTenantTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setTenant;
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

  TenantLifecycle underTest;

  @Before
  @Override
  public void setup() {
    underTest = new TenantLifecycle(licenseManager,
        dataMigrator,
        newInstancePopulator,
        config);
  }

  @Test
  public void shouldBootTenant() throws Exception {
    setTenant("tenant");

    underTest.bootTenant();

    verify(dataMigrator).migrate();
    verify(licenseManager).loadLicense();
    verify(newInstancePopulator).populateIfNewInstance();
  }

  @Test
  public void shouldAttemptToLoadLicenseFile_whenLicenseSpecified() throws Exception {
    setTenant("tenant");

    String sonatypeWorkDir = "tenant/work";
    String licenseFile = "license.lic";

    when(config.getLicenseFile()).thenReturn(licenseFile);
    when(config.getSonatypeWork()).thenReturn(new File(sonatypeWorkDir));

    underTest.bootTenant();

    verify(licenseManager).installLicenseIfUnlicensed(sonatypeWorkDir + "/" + licenseFile);
  }
}
