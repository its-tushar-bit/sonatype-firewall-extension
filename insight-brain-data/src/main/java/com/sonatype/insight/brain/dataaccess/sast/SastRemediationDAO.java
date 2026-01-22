/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastRemediation;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class SastRemediationDAO
    extends AbstractOperationalSqlDAO<SastRemediation>
{
  @Inject
  public SastRemediationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void update(final TransactionContext tx, final SastRemediation entity) {
    throw new UnsupportedOperationException("The SastRemediation table does not support update operations");
  }

  public List<SastRemediation> getBySastFindingId(final String sastFindingId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getBySastFindingId(tx, sastFindingId);
    }
  }

  public List<SastRemediation> getBySastFindingId(final TransactionContext tx, final String sastFindingId) {
    final String sQuery = "SELECT entity FROM SastRemediation entity WHERE entity.sastFindingId=?1";
    return getList(tx, sQuery, sastFindingId);
  }

  public void deleteBySastFindingId(final String sastFindingId) {
    try (final TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteBySastFindingId(tx, sastFindingId);
      tx.commit();
    }
  }

  public void deleteBySastFindingId(final TransactionContext tx, final String sastFindingId) {
    final String sQuery = "DELETE FROM SastRemediation entity WHERE entity.sastFindingId=?1";
    createQuery(sQuery, sastFindingId).executeUpdate(tx);
  }
}
