/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.39
 */
@Named
@Singleton
public class SuccessMetricsReportDataDAO
    extends AbstractAggregationSqlDAO<SuccessMetricsReportData>
{
  @Inject
  public SuccessMetricsReportDataDAO(final AggregationDataStore aggregationDataStore) {
    super(aggregationDataStore);
  }

  @Override
  public SuccessMetricsReportData getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM SuccessMetricsReportData entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }
}
