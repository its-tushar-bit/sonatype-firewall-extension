/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApplicationLifecycle
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycle.class);

  private final InsightConfig configuration;

  private final CLMLicenseManager licenseManager;

  private final DataMigrator dataMigrator;

  private final NewInstancePopulator newInstancePopulator;

  private final LicenseDataUpdater licenseDataUpdater;

  @Inject
  public ApplicationLifecycle(InsightConfig configuration,
                              CLMLicenseManager licenseManager,
                              DataMigrator dataMigrator,
                              NewInstancePopulator newInstancePopulator,
                              DefaultLicenseDataUpdater licenseDataUpdater)
  {
    this.configuration = configuration;
    this.licenseManager = licenseManager;
    this.dataMigrator = dataMigrator;
    this.newInstancePopulator = newInstancePopulator;
    this.licenseDataUpdater = licenseDataUpdater;
  }

  public void boot() throws Exception {
    // If a license is not installed and the config has a license file path, then try to install it from there.
    licenseManager.installLicenseIfUnlicensed(configuration.getLicenseFile());

    LicenseDataUpdater.setUpdater(licenseDataUpdater);

    dataMigrator.migrate();

    // This call must come after the DataMigrator. Specifically, the RootOrganizationConfigMigrator as the sample data
    // will interfere with its decision to determine a fresh install and mistakenly trigger the root org migration.
    newInstancePopulator.populateIfNewInstance();

    new Thread("Startup license data updater")
    {
      @Override
      public void run() {
        try {
          LicenseDataUpdater.update();
        }
        catch (Exception e) {
          log.info("Failed to retrieve license data from Sonatype HDS", log.isDebugEnabled() ? e : null);
        }
      }
    }.start();
  }
}
