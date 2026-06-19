/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.dataaccess.TransactionContext;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ConsumptionEvents.CONSUMPTION_EVENTS;

/**
 * Shared {@link ConsumptionEventDAO} field + count-helpers for the consumption-event
 * integration tests. Counts use the DAO's own {@link TransactionContext} to stay on
 * the same data source as the writes under test.
 *
 * @since 1.205 (CLM-40771)
 */
public abstract class ConsumptionEventIntegrationTestSupport
    extends AbstractDataTest
{
  protected ConsumptionEventDAO dao;

  protected long countWithKey(final String key) {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      long count = tx.dsl()
          .fetchCount(CONSUMPTION_EVENTS, CONSUMPTION_EVENTS.IDEMPOTENCY_KEY.eq(key));
      tx.commit();
      return count;
    }
  }

  protected long countNullKeyRows() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      long count = tx.dsl()
          .fetchCount(CONSUMPTION_EVENTS, CONSUMPTION_EVENTS.IDEMPOTENCY_KEY.isNull());
      tx.commit();
      return count;
    }
  }

  protected long countByActivityType(final ActivityType type) {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      long count = tx.dsl()
          .fetchCount(CONSUMPTION_EVENTS, CONSUMPTION_EVENTS.ACTIVITY_TYPE.eq(type.name()));
      tx.commit();
      return count;
    }
  }
}
