/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantLifecycleTest
    extends MultiTenantTestSupport
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
    testAsNewTenant(t -> {
      underTest.bootTenant();

      verify(dataMigrator).migrate();
      verify(licenseManager).loadLicense();
      verify(newInstancePopulator).populateIfNewInstance();
    });
  }

  @Test
  public void shouldAttemptToLoadLicenseFile_whenLicenseSpecified() throws Exception {
    testAsNewTenant(t -> {
      String sonatypeWorkDir = "tenant" + File.separator + "work";
      String licenseFile = "license.lic";

      when(config.getLicenseFile()).thenReturn(licenseFile);
      when(config.getSonatypeWork()).thenReturn(new File(sonatypeWorkDir));

      underTest.bootTenant();

      verify(licenseManager).installLicenseIfUnlicensed(sonatypeWorkDir + File.separator + licenseFile);
    });
  }
}
