/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.roi;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.aggregation.tables.RoiConfiguration.ROI_CONFIGURATION;

@Named
@Singleton
public class RoiConfigurationDAO
    extends AbstractAggregationSqlDAO<RoiConfiguration>
{
  @Inject
  public RoiConfigurationDAO(AggregationDataStore aggregationDataStore) {
    super(aggregationDataStore);
  }

  public RoiConfiguration getByCurrencyType(CurrencyTypes currencyType) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(ROI_CONFIGURATION)
          .where(ROI_CONFIGURATION.CURRENCY.eq(currencyType.name()))
          .fetchOne());
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return ROI_CONFIGURATION;
  }

  @Override
  public Class<RoiConfiguration> getEntityClass() {
    return RoiConfiguration.class;
  }
}
