/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import datadog.trace.api.Trace;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This runs on when a new tenant is 'booted' and is responsible for initializing anything the tenant needs.
 */
@Named
@Singleton
public class TenantLifecycle
{
  private static final Logger log = LoggerFactory.getLogger(TenantLifecycle.class);

  private final DataMigrator dataMigrator;

  private final CLMLicenseManager licenseManager;

  private final NewInstancePopulator newInstancePopulator;

  private final InsightConfig config;

  @Inject
  public TenantLifecycle(
      CLMLicenseManager licenseManager,
      DataMigrator dataMigrator,
      NewInstancePopulator newInstancePopulator,
      InsightConfig config)
  {
    this.dataMigrator = dataMigrator;
    this.licenseManager = licenseManager;
    this.newInstancePopulator = newInstancePopulator;
    this.config = config;
  }

  @Trace
  public void bootTenant() {
    try {
      log.info("TenantLifecycle start");

      dataMigrator.migrate();
      log.info("TenantLifecycle dataMigrator.migrate() - complete");

      licenseManager.loadLicense();
      log.info("TenantLifecycle licenseManager.loadLicense() - complete");

      maybeLoadLicenseFile();
      log.info("TenantLifecycle maybeLoadLicenseFile() - complete");

      // This call must come after the DataMigrator. Specifically, the RootOrganizationConfigMigrator as the sample data
      // will interfere with its decision to determine a fresh install and mistakenly trigger the root org migration.
      newInstancePopulator.populateIfNewInstance();
      log.info("TenantLifecycle newInstancePopulator.populateIfNewInstance() - complete");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * In a multi tenant environment license files are no longer loaded on startup (with the application lifecycle)
   * instead they are installed/loaded per-tenant. Mostly we expect licenses to be installed via the app itself, however
   * we're retaining this functionality in case we need to use and also to help with integration testing
   */
  private void maybeLoadLicenseFile() {
    try {
      if (config.getLicenseFile() != null) {
        // Cannot make use of config.getLicenseFile() directly because that is per application rather than per tenant
        // The licenseFile attribute in the config.yml has a different purpose in MTIQ and is only the file name, not
        // the path to the file, the file must go into the per-tenant folder in sonatype-work
        licenseManager.installLicenseIfUnlicensed(
            new File(config.getSonatypeWork(), config.getLicenseFile()).getPath());
      }
    }
    catch (Exception e) {
      log.warn("The license {} could not be installed", config.getLicenseFile(), e);
    }
  }
}
