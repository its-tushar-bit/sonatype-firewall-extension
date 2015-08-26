/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.17
 */
public class RepositoryComponentDAO
    extends AbstractOperationalSqlDAO<RepositoryComponent>
{
  @Override
  public RepositoryComponent getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<RepositoryComponent> getByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  public List<RepositoryComponent> getByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1";
    return getList(tx, sQuery, repositoryId);
  }

  public RepositoryComponent getByRepositoryIdAndPathname(String repositoryId, String pathname)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public RepositoryComponent getByRepositoryIdAndPathname(TransactionContext tx, String repositoryId, String pathname)
  {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2";
    return get(tx, sQuery, repositoryId, pathname);
  }
}
