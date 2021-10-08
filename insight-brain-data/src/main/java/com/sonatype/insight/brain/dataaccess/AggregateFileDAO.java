/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;

import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.104
 */
public class AggregateFileDAO
    extends AbstractOperationalSqlDAO<AggregateFile>
{
  @Override
  public AggregateFile getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM AggregateFile entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void update(TransactionContext tx, AggregateFile entity) {
    throw new UnsupportedOperationException("AggregateFile does not support update operations");
  }

  public List<AggregateFile> getByApplicationComponentId(TransactionContext tx, String applicationComponentId) {
    String sQuery = "SELECT entity FROM AggregateFile entity" + //
        " WHERE entity.applicationComponentId=?1";
    return getList(tx, sQuery, applicationComponentId);
  }

  public List<AggregateFile> getByApplicationComponentId(String applicationComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationComponentId(tx, applicationComponentId);
    }
  }

  public void deleteByApplicationComponentId(TransactionContext tx, String applicationComponentId) {
    String sQuery = "DELETE FROM AggregateFile entity" + //
        " WHERE entity.applicationComponentId=?1";
    createQuery(sQuery, applicationComponentId).executeUpdate(tx);
  }

  public void deleteByApplicationComponentId(String applicationComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationComponentId(tx, applicationComponentId);
      tx.commit();
    }
  }

  @Override
  public final void delete(TransactionContext tx, AggregateFile aggregateFile) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all entities associated with an application component.
    super.delete(tx, aggregateFile);
  }

  @Override
  public final void delete(AggregateFile aggregateFile) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all entities associated with an application component.
    super.delete(aggregateFile);
  }
}
