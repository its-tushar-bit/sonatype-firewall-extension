/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class SastScanDAO extends AbstractOperationalSqlDAO<SastScan>
{
  private final SastFindingDAO sastFindingDAO;

  @Inject
  public SastScanDAO(
      final OperationalDataStore operationalDataStore,
      final SastFindingDAO sastFindingDAO)
  {
    super(operationalDataStore);
    this.sastFindingDAO = sastFindingDAO;
  }

  @Override
  public void update(final TransactionContext tx, final SastScan entity) {
    throw new UnsupportedOperationException("The SastScan table does not support update operations");
  }

  @Override
  public void delete(final TransactionContext tx, final SastScan entity) {
    sastFindingDAO.deleteBySastScanId(tx, entity.getId());
    super.delete(tx, entity);
  }

  public List<SastScan> getByApplicationId(final String applicationId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  public List<SastScan> getByApplicationId(final TransactionContext tx, final String applicationId) {
    final String sQuery = "SELECT entity FROM SastScan entity WHERE entity.applicationId=?1";
    return getList(tx, sQuery, applicationId);
  }

  public void deleteByApplicationId(final String applicationId) {
    try (final TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
  }

  public void deleteByApplicationId(final TransactionContext tx, final String applicationId) {
    getByApplicationId(tx, applicationId).forEach(sastScan -> delete(tx, sastScan));
  }
}
