/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.RepositoryMigration;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;
import org.jooq.exception.DataAccessException;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryMigration.REPOSITORY_MIGRATION;

@Named
@Singleton
public class RepositoryMigrationDAO
    extends AbstractOperationalSqlDAO<RepositoryMigration>
{
  @Inject
  public RepositoryMigrationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY_MIGRATION;
  }

  @Override
  public List<RepositoryMigration> getAll(TransactionContext tx) {
    return tx.dsl().selectFrom(REPOSITORY_MIGRATION).fetch(super::toEntity);
  }

  @Override
  public void delete(TransactionContext tx, RepositoryMigration entity) {
    tx.dsl()
        .deleteFrom(REPOSITORY_MIGRATION)
        .where(REPOSITORY_MIGRATION.REPOSITORY_MIGRATION_ID.eq(entity.getId()))
        .execute();
  }

  public RepositoryMigration getByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  public RepositoryMigration getByRepositoryId(TransactionContext tx, String repositoryId) {
    return super.toEntity(tx.dsl()
        .selectFrom(REPOSITORY_MIGRATION)
        .where(REPOSITORY_MIGRATION.REPOSITORY_ID.eq(repositoryId))
        .fetchOne());
  }

  public boolean tryInsert(RepositoryMigration repositoryMigration) {
    try {
      insert(repositoryMigration);
      return true;
    }
    catch (DataAccessException e) {
      // Check for unique constraint violation (duplicate repository_id)
      String message = e.getMessage();
      if (message == null || !message.toLowerCase().contains("unique")) {
        throw e;
      }
    }
    return false;
  }

  @Override
  public Class<RepositoryMigration> getEntityClass() {
    return RepositoryMigration.class;
  }
}
