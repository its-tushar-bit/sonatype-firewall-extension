/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

/**
 * Verifies that the process can access the databases. Actual checks are implemented in sub-classes.
 *
 * Usage: curl -u admin:admin123 http://localhost:8071/healthcheck?pretty=true
 */
abstract class AbstractDbOperationalCheck
    extends AbstractOperationalCheck
{
  protected final OperationalDataStore operationalDataStore;

  protected final DataMartDataStore dataMartDataStore;

  protected final AggregationDataStore aggregationDataStore;

  protected final ThirdPartyScansDataStore thirdPartyScansDataStore;

  protected AbstractDbOperationalCheck(
      final String name,
      final OperationalDataStore operationalDataStore,
      final DataMartDataStore dataMartDataStore,
      final AggregationDataStore aggregationDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    super(name);

    this.operationalDataStore = operationalDataStore;
    this.dataMartDataStore = dataMartDataStore;
    this.aggregationDataStore = aggregationDataStore;
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;
  }

  @Override
  protected Result check() throws Exception {
    ResultBuilder resultBuilder = Result.builder();
    checkConnection(resultBuilder, operationalDataStore);
    checkConnection(resultBuilder, dataMartDataStore);
    checkConnection(resultBuilder, aggregationDataStore);
    checkConnection(resultBuilder, thirdPartyScansDataStore);

    return resultBuilder.build();
  }

  protected abstract void checkConnection(ResultBuilder resultBuilder, DataStore dataStore);
}
