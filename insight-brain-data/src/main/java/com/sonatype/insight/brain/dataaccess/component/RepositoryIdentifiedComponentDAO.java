/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.time.Duration;
import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

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
  public RepositoryIdentifiedComponent getById(TransactionContext tx, String id) {
    return getByHash(tx, id);
  }

  public RepositoryIdentifiedComponent getByHash(String hash) {
    return getById(hash);
  }

  public RepositoryIdentifiedComponent getByHash(TransactionContext tx, String hash) {
    String sQuery = "SELECT entity FROM RepositoryIdentifiedComponent entity" + //
        " WHERE entity.hash=?1";
    return get(tx, sQuery, hash);
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
    String sQuery = "DELETE FROM RepositoryIdentifiedComponent entity" + //
        " WHERE entity.lastAccessTime < ?1";
    createQuery(sQuery, minLastAccessTime).executeUpdate();
  }

  // Visible for testing
  long now() {
    return System.currentTimeMillis();
  }

  public int deleteByHash(String hash) {
    String sQuery = "DELETE FROM RepositoryIdentifiedComponent entity" + //
        " WHERE entity.hash=?1";
    return createQuery(sQuery, hash).executeUpdate();
  }

  public int deleteAll() {
    String sQuery = "DELETE FROM RepositoryIdentifiedComponent entity";
    return createQuery(sQuery).executeUpdate();
  }

  public int deleteByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    String sQuery = "DELETE FROM RepositoryIdentifiedComponent entity" + //
        " WHERE entity.componentIdFormat=?1 AND entity.componentIdCoordinatesJson=?2";
    return createQuery(sQuery, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())).executeUpdate();
  }
}
