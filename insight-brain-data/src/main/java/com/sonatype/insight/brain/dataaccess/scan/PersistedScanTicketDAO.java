/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.scan;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.scan.PersistedScanTicket;
import com.sonatype.insight.dataaccess.TransactionContext;

public class PersistedScanTicketDAO
    extends AbstractOperationalSqlDAO<PersistedScanTicket>
{
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
    String sQuery = "DELETE FROM PersistedScanTicket entity" + //
        " WHERE entity.createTime <= ?1";
    createQuery(sQuery, date).executeUpdate();
  }
}
