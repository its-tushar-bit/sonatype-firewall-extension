/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import io.dropwizard.lifecycle.Managed;

/**
 * This runs on when a new tenant is 'booted' and is responsible for initializing anything the tenant needs.
 */
@Named
@Singleton
public class TenantLifecycle
    implements Managed
{
  private final DataMigrator dataMigrator;

  private final CLMLicenseManager licenseManager;

  private final NewInstancePopulator newInstancePopulator;

  private final InsightConfig config;

  private final OperationalDataStore operationalDataStore;

  private final AggregationDataStore aggregationDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

  @Inject
  public TenantLifecycle(
      CLMLicenseManager licenseManager,
      DataMigrator dataMigrator,
      NewInstancePopulator newInstancePopulator,
      InsightConfig config,
      OperationalDataStore operationalDataStore,
      AggregationDataStore aggregationDataStore,
      DataMartDataStore dataMartDataStore,
      ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    this.dataMigrator = dataMigrator;
    this.licenseManager = licenseManager;
    this.newInstancePopulator = newInstancePopulator;
    this.config = config;
    this.operationalDataStore = operationalDataStore;
    this.aggregationDataStore = aggregationDataStore;
    this.dataMartDataStore = dataMartDataStore;
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;
  }

  public void bootTenant() {
    try {
      getDatabaseProvisionUtils().initializeDatabases(config, new DatabaseConfigProvider(config));

      dataMigrator.migrate();

      licenseManager.loadLicense();

      // This call must come after the DataMigrator. Specifically, the RootOrganizationConfigMigrator as the sample data
      // will interfere with its decision to determine a fresh install and mistakenly trigger the root org migration.
      newInstancePopulator.populateIfNewInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private DatabaseProvisionUtils getDatabaseProvisionUtils() {
    return new DatabaseProvisionUtils(operationalDataStore,
        aggregationDataStore,
        dataMartDataStore,
        thirdPartyScansDataStore);
  }
}
