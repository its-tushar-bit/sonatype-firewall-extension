/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.roi;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class RoiConfigurationDAO
    extends AbstractAggregationSqlDAO<RoiConfiguration>
{
  @Inject
  public RoiConfigurationDAO(AggregationDataStore aggregationDataStore) {
    super(aggregationDataStore);
  }

  @Override
  public RoiConfiguration getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM RoiConfiguration entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<RoiConfiguration> getAll() {
    String sQuery = "SELECT entity FROM RoiConfiguration entity";
    return getList(sQuery);
  }

  public RoiConfiguration getByCurrencyType(CurrencyTypes currencyType) {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT entity FROM RoiConfiguration entity" + //
          " WHERE entity.currency=?1";
      return get(tx, sQuery, currencyType);
    }
  }
}
