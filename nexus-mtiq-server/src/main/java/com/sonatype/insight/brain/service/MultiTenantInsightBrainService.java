/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.MultiTenantAggregationDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataMartDataStore;
import com.sonatype.insight.brain.db.MultiTenantOperationalDataStore;
import com.sonatype.insight.brain.db.MultiTenantThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

public class MultiTenantInsightBrainService
    extends InsightBrainService
{
  public static void main(final String[] args) {
    try {
      setupServerLogging(args);

      if (!validateTempDir()) {
        System.exit(1);
      }

      new MultiTenantInsightBrainService().run(args);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  @Override
  protected DatabaseProvisionUtils createDatabaseProvisionUtils() {
    OperationalDataStore operationalDataStore = new MultiTenantOperationalDataStore();
    AggregationDataStore aggregationDataStore = new MultiTenantAggregationDataStore();
    DataMartDataStore dataMartDataStore = new MultiTenantDataMartDataStore();
    ThirdPartyScansDataStore thirdPartyScansDataStore = new MultiTenantThirdPartyScansDataStore();

    // Populate the legacy classes
    OperationalDataStoreProvider.setInstance(operationalDataStore);
    AggregationDataStoreProvider.setInstance(aggregationDataStore);
    DatamartProvider.setInstance(dataMartDataStore);
    ThirdPartyScansProvider.setInstance(thirdPartyScansDataStore);

    return new DatabaseProvisionUtils(operationalDataStore, aggregationDataStore, dataMartDataStore,
        thirdPartyScansDataStore);
  }
}
