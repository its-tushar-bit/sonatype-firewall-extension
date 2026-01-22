/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.RollbackException;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.RepositoryMigration;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class RepositoryMigrationDAO
    extends AbstractOperationalSqlDAO<RepositoryMigration>
{
  @Inject
  public RepositoryMigrationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public RepositoryMigration getByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  public RepositoryMigration getByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryMigration entity" + //
        " WHERE entity.repositoryId=?1";
    return get(tx, sQuery, repositoryId);
  }

  public boolean tryInsert(RepositoryMigration repositoryMigration) {
    try {
      insert(repositoryMigration);
      return true;
    }
    catch (RollbackException e) {
      if (!(e.getCause() instanceof EntityExistsException)) {
        throw e;
      }
    }
    return false;
  }
}
