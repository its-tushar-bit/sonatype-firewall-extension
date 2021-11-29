/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.dataaccess.TransactionContext;

public class RepositoryConnectionDAO
    extends AbstractOperationalSqlDAO<RepositoryConnection>
{
  private final OwnerDAO ownerDAO = new OwnerDAO();

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

  public List<RepositoryConnection> getByOwnerIdWithHierarchy(String ownerId) {
    List<RepositoryConnection> repositoryConnections = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      repositoryConnections.addAll(getByOwnerId(owner.getId()));
      if (!repositoryConnections.isEmpty()) {
        break;
      }
    }
    return repositoryConnections;
  }

  public List<RepositoryConnection> getByOwnerIdAndFormatsWithHierarchy(String ownerId, RepositoryFormat... formats) {
    List<RepositoryConnection> repositoryConnections = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      repositoryConnections.addAll(getByOwnerIdAndFormats(owner.getId(), formats));
      if (!repositoryConnections.isEmpty()) {
        break;
      }
    }
    return repositoryConnections;
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
}
