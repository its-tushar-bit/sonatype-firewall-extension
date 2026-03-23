/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ZscalerFormat.ZSCALER_FORMAT;

@Named
@Singleton
public class ZscalerFormatDAO
    extends AbstractOperationalSqlDAO<ZscalerFormat>
{
  @Inject
  public ZscalerFormatDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public List<ZscalerFormat> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(ZSCALER_FORMAT)
          .fetchInto(ZscalerFormat.class);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return ZSCALER_FORMAT;
  }

  @Override
  public Class<ZscalerFormat> getEntityClass() {
    return ZscalerFormat.class;
  }
}
