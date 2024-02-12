/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

public abstract class AbstractDatabaseMigrator
    implements DatabaseMigrator
{
  protected final OperationalDataStore operationalDataStore;

  protected final AggregationDataStore aggregationDataStore;

  protected final DataMartDataStore dataMartDataStore;

  protected final ThirdPartyScansDataStore thirdPartyScansDataStore;

  private final DataStoreMigrator operationalDataStoreMigrator;

  private final DataStoreMigrator aggregationDataStoreMigrator;

  private final DataStoreMigrator dataMartDataStoreMigrator;

  private final DataStoreMigrator thirdPartyScansDataStoreMigrator;

  public AbstractDatabaseMigrator(final DataStoreProvider dataStoreProvider) {
    this(dataStoreProvider.getOperationalDataStore(), dataStoreProvider.getAggregationDataStore(),
        dataStoreProvider.getDataMartDataStore(), dataStoreProvider.getThirdPartyScansDataStore());
  }

  public AbstractDatabaseMigrator(
      OperationalDataStore operationalDataStore,
      AggregationDataStore aggregationDataStore,
      DataMartDataStore dataMartDataStore,
      ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    this.operationalDataStore = operationalDataStore;
    this.aggregationDataStore = aggregationDataStore;
    this.dataMartDataStore = dataMartDataStore;
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;

    this.operationalDataStoreMigrator = createDataStoreMigrator(operationalDataStore);
    this.aggregationDataStoreMigrator = createDataStoreMigrator(aggregationDataStore);
    this.dataMartDataStoreMigrator = createDataStoreMigrator(dataMartDataStore);
    this.thirdPartyScansDataStoreMigrator = createDataStoreMigrator(thirdPartyScansDataStore);
  }

  @Override
  public void migrate() {
    if (isMigrationNeeded()) {
      operationalDataStoreMigrator.migrate();
      aggregationDataStoreMigrator.migrate();
      dataMartDataStoreMigrator.migrate();
      thirdPartyScansDataStoreMigrator.migrate();
    }
  }

  protected abstract boolean isMigrationNeeded();

  protected abstract DataStoreMigrator createDataStoreMigrator(final DataStore dataStore);
}
