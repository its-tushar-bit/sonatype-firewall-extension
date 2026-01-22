/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import static java.util.stream.Collectors.toMap;

/**
 * @since 1.63
 */
@Named
@Singleton
public class DataRetentionPolicyDAO
    extends AbstractOperationalSqlDAO<DataRetentionPolicy>
{
  @Inject
  public DataRetentionPolicyDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public List<DataRetentionPolicy> getAll() {
    String sQuery = "SELECT entity FROM DataRetentionPolicy entity ORDER BY entity.ownerId, entity.contextId";
    return getList(sQuery);
  }

  private void validate(DataRetentionPolicy entity) {
    if (entity.getMaxCount() != null) {
      if (entity.getMaxCount() <= 0) {
        throw new BadRequestException("Maximum count must be positive.");
      }
      if (entity.getMaxCount() >= 10000) {
        throw new BadRequestException("Maximum count must be less than 10000.");
      }
    }
    if (entity.getMaxAgeInDays() != null) {
      if (entity.getMaxAgeInDays() <= 0) {
        throw new BadRequestException("Maximum age must be positive.");
      }
      if (entity.getMaxAgeInDays() >= 50 * 365) {
        throw new BadRequestException("Maximum age must be less than 50 years.");
      }
    }
    if (entity.isPurgingEnabled() && entity.getMaxAgeInDays() == null && entity.getMaxCount() == null) {
      throw new BadRequestException("Cannot enable data purging without criteria for what to purge.");
    }
  }

  @Override
  public void insert(TransactionContext tx, DataRetentionPolicy entity) {
    validate(entity);
    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, DataRetentionPolicy entity) {
    validate(entity);
    super.update(tx, entity);
  }

  public Map<String, DataRetentionPolicy> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public Map<String, DataRetentionPolicy> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM DataRetentionPolicy entity WHERE entity.ownerId = ?1";
    return getList(tx, sQuery, ownerId).stream().collect(toMap(DataRetentionPolicy::getContextId, Function.identity()));
  }

  public DataRetentionPolicy getByOwnerIdAndContextId(String ownerId, String contextId) {
    String sQuery = "SELECT entity FROM DataRetentionPolicy entity WHERE entity.ownerId = ?1 AND entity.contextId = ?2";
    return get(sQuery, ownerId, contextId);
  }
}
