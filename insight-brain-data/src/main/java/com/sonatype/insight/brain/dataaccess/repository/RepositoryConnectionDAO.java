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
    String sQuery = "SELECT entity FROM RepositoryConnection entity" + //
        " WHERE entity.ownerId=?1 AND entity.format IN (?2)";
    return getList(tx, sQuery, ownerId, formats);
  }

  public RepositoryConnection getByOwnerIdAndBaseUrl(String ownerId, String baseUrl) {
    String sQuery = "SELECT entity FROM RepositoryConnection entity" + //
        " WHERE entity.ownerId=?1 AND entity.baseUrl=?2";
    return get(sQuery, ownerId, baseUrl);
  }

  public RepositoryConnection getByOwnerIdAndFormat(String ownerId, RepositoryFormat format) {
    String sQuery = "SELECT entity FROM RepositoryConnection entity" + //
        " WHERE entity.ownerId=?1 AND entity.format=?2";
    return get(sQuery, ownerId, format);
  }

  public RepositoryConnection getByIdAndOwnerId(String repositoryConnectionId, String ownerId) {
    String sQuery = "SELECT entity FROM RepositoryConnection entity" + //
        " WHERE entity.id=?1 AND entity.ownerId=?2";
    return get(sQuery, repositoryConnectionId, ownerId);
  }

  public void deleteAll() {
    String sQuery = "DELETE FROM RepositoryConnection entity";
    createQuery(sQuery).executeUpdate();
  }
}
