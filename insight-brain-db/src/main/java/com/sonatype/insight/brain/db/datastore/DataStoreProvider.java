/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

/**
 * Simple provider to a class that can return the data stores
 */
public interface DataStoreProvider
{
  OperationalDataStore getOperationalDataStore();

  AggregationDataStore getAggregationDataStore();

  DataMartDataStore getDataMartDataStore();

  ThirdPartyScansDataStore getThirdPartyScansDataStore();
}
