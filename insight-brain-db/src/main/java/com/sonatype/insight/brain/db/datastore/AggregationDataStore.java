/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

/**
 * Contract for the aggregation data store.
 */
public interface AggregationDataStore
    extends DataStore
{
  String ID = "insight_brain_aggregation";

  @Override
  default String getID() {
    return ID;
  }
}
