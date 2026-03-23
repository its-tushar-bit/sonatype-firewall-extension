/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ComponentChangeDetectionEvent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentChangeDetectionEvent.COMPONENT_CHANGE_DETECTION_EVENT;

/**
 * @since 1.188.0
 */
@Named
@Singleton
public class ComponentChangeDetectionEventDAO
    extends AbstractOperationalSqlDAO<ComponentChangeDetectionEvent>
{
  private static final Logger log = LoggerFactory.getLogger(ComponentChangeDetectionEventDAO.class);

  @Inject
  protected ComponentChangeDetectionEventDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public void deleteEntriesOlderThan(final Date time) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .deleteFrom(COMPONENT_CHANGE_DETECTION_EVENT)
          .where(COMPONENT_CHANGE_DETECTION_EVENT.ADDED_TIME.lt(time))
          .execute();
      tx.commit();
    }
  }

  public void removeExcessEvents(final long maxEvents) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      long count = getTotalEventCount(tx);
      if (count > maxEvents) {
        doRemoveExcessEvents(tx, count - maxEvents);
      }
      tx.commit();
    }
    catch (Exception e) {
      log.error("Error when deleting event", e);
    }
  }

  private long getTotalEventCount(final TransactionContext tx) {
    return tx.dsl()
        .selectCount()
        .from(COMPONENT_CHANGE_DETECTION_EVENT)
        .fetchOne(0, Long.class);
  }

  private void doRemoveExcessEvents(final TransactionContext tx, final long excessCount) {
    // Get IDs of oldest events to delete
    List<String> idsToDelete = tx.dsl()
        .select(COMPONENT_CHANGE_DETECTION_EVENT.COMPONENT_CHANGE_DETECTION_EVENT_ID)
        .from(COMPONENT_CHANGE_DETECTION_EVENT)
        .orderBy(COMPONENT_CHANGE_DETECTION_EVENT.ADDED_TIME.asc())
        .limit((int) excessCount)
        .fetchInto(String.class);

    if (!idsToDelete.isEmpty()) {
      tx.dsl()
          .deleteFrom(COMPONENT_CHANGE_DETECTION_EVENT)
          .where(COMPONENT_CHANGE_DETECTION_EVENT.COMPONENT_CHANGE_DETECTION_EVENT_ID.in(idsToDelete))
          .execute();
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_CHANGE_DETECTION_EVENT;
  }

  @Override
  public Class<ComponentChangeDetectionEvent> getEntityClass() {
    return ComponentChangeDetectionEvent.class;
  }
}
