/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.mockito.Mockito.inOrder;

public class ApplicationLifecycleTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationLifecycle lifecycle;

  @Mock
  private DataMigrator dataMigrator;

  @Mock
  private CLMLicenseManager licenseManager;

  @Mock
  private DefaultLicenseDataUpdater licenseDataUpdater;

  @Mock
  private NewInstancePopulator newInstancePopulator;

  @Override
  public void configure(Binder binder) {
    binder.bind(DataMigrator.class).toInstance(dataMigrator);
    binder.bind(CLMLicenseManager.class).toInstance(licenseManager);
    binder.bind(DefaultLicenseDataUpdater.class).toInstance(licenseDataUpdater);
    binder.bind(NewInstancePopulator.class).toInstance(newInstancePopulator);
    super.configure(binder);
  }

  @Test
  public void testBoot_MigrateProxyConfigurationBeforeLicenseRegistrationWithHds() throws Exception {
    lifecycle.boot();

    InOrder inOrder = inOrder(dataMigrator, licenseManager);
    inOrder.verify(dataMigrator).migrate();
    inOrder.verify(licenseManager).loadLicense();
  }
}
