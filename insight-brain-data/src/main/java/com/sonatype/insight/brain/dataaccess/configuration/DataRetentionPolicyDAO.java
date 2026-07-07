/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.DataRetentionPolicy.DATA_RETENTION_POLICY;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;
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
  public DataRetentionPolicyDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public int insert(final TransactionContext tx, final DataRetentionPolicy entity) {
    validate(entity);
    return super.insert(tx, entity);
  }

  @Override
  public int update(final TransactionContext tx, final DataRetentionPolicy entity) {
    validate(entity);
    return super.update(tx, entity);
  }

  @Override
  public List<DataRetentionPolicy> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(DATA_RETENTION_POLICY)
          .orderBy(DATA_RETENTION_POLICY.OWNER_ID, DATA_RETENTION_POLICY.CONTEXT_ID)
          .fetch()
          .stream()
          .map(this::toEntity)
          .collect(Collectors.toList());
    }
  }

  public Map<String, DataRetentionPolicy> getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public Map<String, DataRetentionPolicy> getByOwnerId(final TransactionContext tx, final String ownerId) {
    return tx.dsl()
        .selectFrom(DATA_RETENTION_POLICY)
        .where(DATA_RETENTION_POLICY.OWNER_ID.eq(ownerId))
        .fetch()
        .stream()
        .map(this::toEntity)
        .collect(toMap(DataRetentionPolicy::getContextId, Function.identity()));
  }

  public DataRetentionPolicy getByOwnerIdAndContextIdWithHierarchy(String ownerId, String contextId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .select(DATA_RETENTION_POLICY.fields())
          .from(DATA_RETENTION_POLICY)
          .join(OWNER_ANCESTOR)
          .on(DATA_RETENTION_POLICY.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
          .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
          .and(DATA_RETENTION_POLICY.CONTEXT_ID.eq(contextId))
          .orderBy(OWNER_ANCESTOR.ANCESTOR_DISTANCE)
          .limit(1)
          .fetchOne());
    }
  }

  public DataRetentionPolicy getByOwnerIdAndContextId(final String ownerId, final String contextId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(DATA_RETENTION_POLICY)
          .where(DATA_RETENTION_POLICY.OWNER_ID.eq(ownerId))
          .and(DATA_RETENTION_POLICY.CONTEXT_ID.eq(contextId))
          .fetchOne());
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return DATA_RETENTION_POLICY;
  }

  @Override
  public Class<DataRetentionPolicy> getEntityClass() {
    return DataRetentionPolicy.class;
  }

  private void validate(final DataRetentionPolicy entity) {
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
}
