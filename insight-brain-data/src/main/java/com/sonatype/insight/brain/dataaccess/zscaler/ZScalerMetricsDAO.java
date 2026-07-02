/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.zscaler;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.zscaler.ZScalerMetrics;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ZscalerMetrics.ZSCALER_METRICS;

@Named
@Singleton
public class ZScalerMetricsDAO
    extends AbstractOperationalSqlDAO<ZScalerMetrics>
{
  public static final String SINGLETON_ENTITY_ID = "zscaler-metrics";

  @Inject
  public ZScalerMetricsDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * @return The Zscaler metrics data or {@code null} if none.
   */
  public ZScalerMetrics get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public void set(ZScalerMetrics zScalerMetrics) {
    update(zScalerMetrics);
  }

  @Override
  public int insert(TransactionContext tx, ZScalerMetrics zScalerMetrics) {
    zScalerMetrics.setId(SINGLETON_ENTITY_ID);
    zScalerMetrics.setUpdatedAt(new Date());
    return super.insert(tx, zScalerMetrics);
  }

  @Override
  public void update(TransactionContext tx, ZScalerMetrics zScalerMetrics) {
    zScalerMetrics.setId(SINGLETON_ENTITY_ID);
    zScalerMetrics.setUpdatedAt(new Date());

    // Use upsert semantics since set() calls update() directly without checking if row exists
    ZScalerMetrics existing = getById(tx, SINGLETON_ENTITY_ID);
    if (existing != null) {
      super.update(tx, zScalerMetrics);
    }
    else {
      super.insert(tx, zScalerMetrics);
    }
  }

  @Override
  public ZScalerMetrics getById(TransactionContext tx, String id) {
    return super.getById(tx, SINGLETON_ENTITY_ID);
  }

  @Override
  public Table<?> getJooqTable() {
    return ZSCALER_METRICS;
  }

  @Override
  public Class<ZScalerMetrics> getEntityClass() {
    return ZScalerMetrics.class;
  }
}
