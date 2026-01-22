/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ComponentChangeDetectionEvent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public void deleteEntriesOlderThan(Date time) {
    String sQuery = "DELETE FROM ComponentChangeDetectionEvent entity WHERE entity.addedTime < ?1";
    createQuery(sQuery, time).executeUpdate();
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

  private long getTotalEventCount(TransactionContext tx) {
    String sQuery = "SELECT COUNT(*) FROM " + getDatabaseSchema() + ".component_change_detection_event";
    return (long) tx.createNativeQuery(sQuery).getSingleResult();
  }

  private void doRemoveExcessEvents(TransactionContext tx, long excessCount) {
    String sQuery = "DELETE FROM " + getDatabaseSchema() + ".component_change_detection_event" +
        " WHERE added_time IN (SELECT added_time FROM " + getDatabaseSchema() + ".component_change_detection_event" +
        " ORDER BY added_time ASC LIMIT ?1)";
    tx.createNativeQuery(sQuery).setParameter(1, excessCount).executeUpdate();
  }
}
