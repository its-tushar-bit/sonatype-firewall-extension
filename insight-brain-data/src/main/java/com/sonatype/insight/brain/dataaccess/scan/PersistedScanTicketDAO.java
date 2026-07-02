/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.scan;

import java.util.Date;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.scan.PersistedScanTicket;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PersistedScanTicket.PERSISTED_SCAN_TICKET;

@Named
@Singleton
public class PersistedScanTicketDAO
    extends AbstractOperationalSqlDAO<PersistedScanTicket>
{
  @Inject
  public PersistedScanTicketDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public final void delete(TransactionContext tx, PersistedScanTicket persistedScanTicket) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(tx, persistedScanTicket);
  }

  @Override
  public final void delete(PersistedScanTicket persistedScanTicket) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(persistedScanTicket);
  }

  public void deleteBeforeOrOn(Date date) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .deleteFrom(PERSISTED_SCAN_TICKET)
          .where(PERSISTED_SCAN_TICKET.CREATE_TIME.le(date))
          .execute();
      tx.commit();
    }
  }

  @Override
  public int insert(TransactionContext tx, PersistedScanTicket entity) {
    if (entity.getCreateTime() == null) {
      entity.setCreateTime(new Date());
    }
    return super.insert(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return PERSISTED_SCAN_TICKET;
  }

  @Override
  public Class<PersistedScanTicket> getEntityClass() {
    return PersistedScanTicket.class;
  }
}
