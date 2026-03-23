/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.roi;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfigurationDefaultValues;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.aggregation.tables.RoiConfigurationDefaultValues.ROI_CONFIGURATION_DEFAULT_VALUES;

@Named
@Singleton
public class RoiConfigurationDefaultValuesDAO
    extends AbstractAggregationSqlDAO<RoiConfigurationDefaultValues>
{
  @Inject
  public RoiConfigurationDefaultValuesDAO(AggregationDataStore aggregationDataStore) {
    super(aggregationDataStore);
  }

  public RoiConfigurationDefaultValues getByCurrencyType(CurrencyTypes currencyType) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(ROI_CONFIGURATION_DEFAULT_VALUES)
          .where(ROI_CONFIGURATION_DEFAULT_VALUES.CURRENCY.eq(currencyType.name()))
          .fetchOneInto(RoiConfigurationDefaultValues.class);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return ROI_CONFIGURATION_DEFAULT_VALUES;
  }

  @Override
  public Class<RoiConfigurationDefaultValues> getEntityClass() {
    return RoiConfigurationDefaultValues.class;
  }
}
