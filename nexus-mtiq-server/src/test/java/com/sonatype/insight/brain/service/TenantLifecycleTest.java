/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantLifecycleTest
    extends AbstractMultiTenantTest
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

  @BeforeEach
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
