/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.aggregation.tables.SuccessMetricsReportData.SUCCESS_METRICS_REPORT_DATA;

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
  public Table<?> getJooqTable() {
    return SUCCESS_METRICS_REPORT_DATA;
  }

  @Override
  public Class<SuccessMetricsReportData> getEntityClass() {
    return SuccessMetricsReportData.class;
  }
}
