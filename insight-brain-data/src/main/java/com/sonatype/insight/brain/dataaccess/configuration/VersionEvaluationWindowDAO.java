/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.VersionEvaluationWindow;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class VersionEvaluationWindowDAO
    extends AbstractOperationalSqlDAO<VersionEvaluationWindow>
{
  @Inject
  public VersionEvaluationWindowDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  private void validate(final VersionEvaluationWindow entity) {
    if (entity == null) {
      throw new BadRequestException("entity cannot be null.");
    }
    if (entity.getOwnerId() == null) {
      throw new BadRequestException("ownerId is required.");
    }
    if (entity.getContextId() == null) {
      throw new BadRequestException("contextId is required.");
    }
    if (entity.getMaxVersions() == null && entity.getMaxAgeInDays() == null) {
      throw new BadRequestException("At least one of maxVersions or maxAgeInDays must be specified.");
    }
    if (entity.getMaxVersions() != null) {
      if (entity.getMaxVersions() < 0) {
        throw new BadRequestException("maxVersions cannot be negative.");
      }
    }
    if (entity.getMaxAgeInDays() != null) {
      if (entity.getMaxAgeInDays() < 0) {
        throw new BadRequestException("maxAgeInDays cannot be negative.");
      }
    }
  }

  @Override
  public void insert(final TransactionContext tx, final VersionEvaluationWindow entity) {
    validate(entity);
    super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final VersionEvaluationWindow entity) {
    validate(entity);
    super.update(tx, entity);
  }

  public List<VersionEvaluationWindow> getByOwnerId(final String ownerId) {
    String sQuery = "SELECT entity FROM VersionEvaluationWindow entity WHERE entity.ownerId = ?1";
    return getList(sQuery, ownerId);
  }

  public VersionEvaluationWindow getByOwnerIdAndContextId(final String ownerId, final String contextId) {
    String sQuery =
        "SELECT entity FROM VersionEvaluationWindow entity WHERE entity.ownerId = ?1 AND entity.contextId = ?2";
    return get(sQuery, ownerId, contextId);
  }

  public void deleteByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByOwnerId(tx, ownerId);
      tx.commit();
    }
  }

  public void deleteByOwnerId(final TransactionContext tx, final String ownerId) {
    String sQuery = "DELETE FROM VersionEvaluationWindow entity WHERE entity.ownerId = ?1";
    createQuery(tx, sQuery, ownerId).executeUpdate();
  }

  public void deleteByOwnerIdAndContextId(final String ownerId, final String contextId) {
    String sQuery = "DELETE FROM VersionEvaluationWindow entity WHERE entity.ownerId = ?1 AND entity.contextId = ?2";
    createQuery(sQuery, ownerId, contextId).executeUpdate();
  }
}
