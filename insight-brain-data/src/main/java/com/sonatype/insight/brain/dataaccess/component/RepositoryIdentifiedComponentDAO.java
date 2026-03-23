/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.time.Duration;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryIdentifiedComponent.REPOSITORY_IDENTIFIED_COMPONENT;

@Named
@Singleton
public class RepositoryIdentifiedComponentDAO
    extends AbstractOperationalSqlDAO<RepositoryIdentifiedComponent>
{
  @Inject
  public RepositoryIdentifiedComponentDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void update(TransactionContext tx, RepositoryIdentifiedComponent entity) {
    // Use upsert semantics since RepositoryIdentifiedComponentCache.put() calls update() directly
    // without checking if row exists
    if (getById(tx, entity.getId()) == null) {
      insert(tx, entity);
    }
    else {
      super.update(tx, entity);
    }
  }

  public RepositoryIdentifiedComponent getByHash(String hash) {
    return getById(hash);
  }

  public RepositoryIdentifiedComponent getByHash(TransactionContext tx, String hash) {
    return getById(tx, hash);
  }

  public RepositoryIdentifiedComponent getByHashNotNullAndUpdateLastAccessTime(String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      RepositoryIdentifiedComponent repositoryIdentifiedComponent =
          getByHashNotNullAndUpdateLastAccessTime(tx, hash);
      tx.commit();
      return repositoryIdentifiedComponent;
    }
  }

  public RepositoryIdentifiedComponent getByHashNotNullAndUpdateLastAccessTime(TransactionContext tx, String hash) {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = getByHash(tx, hash);
    if (repositoryIdentifiedComponent == null) {
      throw new NotFoundException("RepositoryIdentifiedComponent with hash " + hash + " does not exist.");
    }
    repositoryIdentifiedComponent.setLastAccessTime(new Date());
    update(tx, repositoryIdentifiedComponent);
    return repositoryIdentifiedComponent;
  }

  public void deleteInfrequentlyAccessed(Duration maxLastAccess) {
    Date minLastAccessTime = new Date(now() - maxLastAccess.toMillis());
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .deleteFrom(REPOSITORY_IDENTIFIED_COMPONENT)
          .where(REPOSITORY_IDENTIFIED_COMPONENT.LAST_ACCESS_TIME.lessThan(minLastAccessTime))
          .execute();
      tx.commit();
    }
  }

  // Visible for testing
  long now() {
    return System.currentTimeMillis();
  }

  public int deleteByHash(String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int count = tx.dsl()
          .deleteFrom(REPOSITORY_IDENTIFIED_COMPONENT)
          .where(REPOSITORY_IDENTIFIED_COMPONENT.HASH.eq(hash))
          .execute();
      tx.commit();
      return count;
    }
  }

  public int deleteAll() {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int count = tx.dsl()
          .deleteFrom(REPOSITORY_IDENTIFIED_COMPONENT)
          .execute();
      tx.commit();
      return count;
    }
  }

  public int deleteByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int count = tx.dsl()
          .deleteFrom(REPOSITORY_IDENTIFIED_COMPONENT)
          .where(REPOSITORY_IDENTIFIED_COMPONENT.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat())
              .and(REPOSITORY_IDENTIFIED_COMPONENT.COMPONENT_ID_COORDINATES_JSON.eq(
                  ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()))))
          .execute();
      tx.commit();
      return count;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY_IDENTIFIED_COMPONENT;
  }

  @Override
  public Class<RepositoryIdentifiedComponent> getEntityClass() {
    return RepositoryIdentifiedComponent.class;
  }
}
