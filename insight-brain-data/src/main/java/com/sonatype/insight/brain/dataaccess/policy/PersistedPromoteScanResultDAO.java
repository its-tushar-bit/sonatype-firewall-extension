/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PersistedPromoteScanResult;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

public class PersistedPromoteScanResultDAO
    extends AbstractOperationalSqlDAO<PersistedPromoteScanResult>
{
  @Override
  public PersistedPromoteScanResult getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM PersistedPromoteScanResult entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public PersistedPromoteScanResult getByIdNotNull(String id) {
    PersistedPromoteScanResult persistedPromoteScanResult = getById(id);
    if (persistedPromoteScanResult == null) {
      throw new NotFoundException("Could not find a PersistedPromoteScanResult with ID " + id + ".");
    }
    return persistedPromoteScanResult;
  }

  public List<PersistedPromoteScanResult> getAll() {
    String sQuery = "SELECT entity FROM PersistedPromoteScanResult entity";
    return getList(sQuery);
  }

  @Override
  public final void delete(TransactionContext tx, PersistedPromoteScanResult entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(tx, entity);
  }

  @Override
  public final void delete(PersistedPromoteScanResult entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(entity);
  }

  public void deleteBeforeOrOn(Date date) {
    String sQuery = "DELETE FROM PersistedPromoteScanResult entity" + //
        " WHERE entity.createTime <= ?1";
    createQuery(sQuery, date).executeUpdate();
  }
}
