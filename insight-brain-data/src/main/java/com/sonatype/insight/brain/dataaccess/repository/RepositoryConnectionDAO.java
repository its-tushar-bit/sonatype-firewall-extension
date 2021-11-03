/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.dataaccess.TransactionContext;

public class RepositoryConnectionDAO
    extends AbstractOperationalSqlDAO<RepositoryConnection>
{
  @Override
  public RepositoryConnection getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM RepositoryConnection entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<RepositoryConnection> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<RepositoryConnection> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM RepositoryConnection entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }
}
