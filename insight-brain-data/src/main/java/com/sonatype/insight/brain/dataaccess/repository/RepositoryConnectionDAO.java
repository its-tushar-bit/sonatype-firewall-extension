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
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryConnection.REPOSITORY_CONNECTION;

@Named
@Singleton
public class RepositoryConnectionDAO
    extends AbstractOperationalSqlDAO<RepositoryConnection>
    implements RotatableSecrets
{
  @Inject
  public RepositoryConnectionDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY_CONNECTION;
  }

  @Override
  public List<RepositoryConnection> getAll(TransactionContext tx) {
    return tx.dsl()
        .selectFrom(REPOSITORY_CONNECTION)
        .fetch(this::toEntity);
  }

  public List<RepositoryConnection> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<RepositoryConnection> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(REPOSITORY_CONNECTION)
        .where(REPOSITORY_CONNECTION.OWNER_ID.eq(ownerId))
        .fetch(this::toEntity);
  }

  public List<RepositoryConnection> getByOwnerIdAndFormats(String ownerId, RepositoryFormat... formats) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndFormats(tx, ownerId, formats);
    }
  }

  public List<RepositoryConnection> getByOwnerIdAndFormats(
      TransactionContext tx,
      String ownerId,
      RepositoryFormat... formats)
  {
    String[] formatNames = java.util.Arrays.stream(formats)
        .map(RepositoryFormat::name)
        .toArray(String[]::new);
    return tx.dsl()
        .selectFrom(REPOSITORY_CONNECTION)
        .where(REPOSITORY_CONNECTION.OWNER_ID.eq(ownerId))
        .and(REPOSITORY_CONNECTION.FORMAT.in(formatNames))
        .fetch(this::toEntity);
  }

  public RepositoryConnection getByOwnerIdAndBaseUrl(String ownerId, String baseUrl) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(REPOSITORY_CONNECTION)
          .where(REPOSITORY_CONNECTION.OWNER_ID.eq(ownerId))
          .and(REPOSITORY_CONNECTION.BASE_URL.eq(baseUrl))
          .fetchOne());
    }
  }

  public RepositoryConnection getByOwnerIdAndFormat(String ownerId, RepositoryFormat format) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(REPOSITORY_CONNECTION)
          .where(REPOSITORY_CONNECTION.OWNER_ID.eq(ownerId))
          .and(REPOSITORY_CONNECTION.FORMAT.eq(format.name()))
          .fetchOne());
    }
  }

  public RepositoryConnection getByIdAndOwnerId(String repositoryConnectionId, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(REPOSITORY_CONNECTION)
          .where(REPOSITORY_CONNECTION.REPOSITORY_CONNECTION_ID.eq(repositoryConnectionId))
          .and(REPOSITORY_CONNECTION.OWNER_ID.eq(ownerId))
          .fetchOne());
    }
  }

  public void deleteAll() {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl().deleteFrom(REPOSITORY_CONNECTION).execute();
      tx.commit();
    }
  }

  @Override
  public void delete(TransactionContext tx, RepositoryConnection entity) {
    tx.dsl()
        .deleteFrom(REPOSITORY_CONNECTION)
        .where(REPOSITORY_CONNECTION.REPOSITORY_CONNECTION_ID.eq(entity.getId()))
        .execute();
  }

  @Override
  public Class<RepositoryConnection> getEntityClass() {
    return RepositoryConnection.class;
  }
}
