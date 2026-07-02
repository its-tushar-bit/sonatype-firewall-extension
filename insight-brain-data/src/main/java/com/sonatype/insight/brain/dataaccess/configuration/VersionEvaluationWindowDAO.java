/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.VersionEvaluationWindow;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.Tables.OWNER_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.VersionEvaluationWindow.VERSION_EVALUATION_WINDOW;

@Named
@Singleton
public class VersionEvaluationWindowDAO
    extends AbstractOperationalSqlDAO<VersionEvaluationWindow>
{
  @Inject
  public VersionEvaluationWindowDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return VERSION_EVALUATION_WINDOW;
  }

  @Override
  public Class<VersionEvaluationWindow> getEntityClass() {
    return VersionEvaluationWindow.class;
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
  public int insert(final TransactionContext tx, final VersionEvaluationWindow entity) {
    validate(entity);
    return super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final VersionEvaluationWindow entity) {
    validate(entity);
    super.update(tx, entity);
  }

  public List<VersionEvaluationWindow> getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(VERSION_EVALUATION_WINDOW)
          .where(VERSION_EVALUATION_WINDOW.OWNER_ID.eq(ownerId))
          .fetch(this::toEntity);
    }
  }

  public VersionEvaluationWindow getByOwnerIdAndContextId(final String ownerId, final String contextId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(VERSION_EVALUATION_WINDOW)
          .where(VERSION_EVALUATION_WINDOW.OWNER_ID.eq(ownerId))
          .and(VERSION_EVALUATION_WINDOW.CONTEXT_ID.eq(contextId))
          .fetchOne());
    }
  }

  public void deleteByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByOwnerId(tx, ownerId);
      tx.commit();
    }
  }

  public void deleteByOwnerId(final TransactionContext tx, final String ownerId) {
    tx.dsl()
        .deleteFrom(VERSION_EVALUATION_WINDOW)
        .where(VERSION_EVALUATION_WINDOW.OWNER_ID.eq(ownerId))
        .execute();
  }

  public void deleteByOwnerIdAndContextId(final String ownerId, final String contextId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .deleteFrom(VERSION_EVALUATION_WINDOW)
          .where(VERSION_EVALUATION_WINDOW.OWNER_ID.eq(ownerId))
          .and(VERSION_EVALUATION_WINDOW.CONTEXT_ID.eq(contextId))
          .execute();
      tx.commit();
    }
  }

  public Map<String, VersionEvaluationWindow> getByOwnerIdsAndContextIdWithInheritance(
      final Set<String> ownerIds,
      final String contextId)
  {
    if (CollectionUtils.isEmpty(ownerIds)) {
      return new HashMap<>();
    }

    var oa = OWNER_ANCESTOR;
    var vew = VERSION_EVALUATION_WINDOW;

    try (TransactionContext tx = createTransactionContext()) {
      Map<String, AbstractMap.SimpleEntry<Integer, VersionEvaluationWindow>> closest = new HashMap<>();

      tx.dsl()
          .select(
              Stream.concat(
                  Stream.of(oa.OWNER_ID, oa.ANCESTOR_DISTANCE),
                  Arrays.stream(vew.fields())).toList())
          .from(oa)
          .join(vew)
          .on(vew.OWNER_ID.eq(oa.ANCESTOR_ID))
          .where(oa.OWNER_ID.in(ownerIds))
          .and(vew.CONTEXT_ID.eq(contextId))
          .fetch()
          .forEach(r -> {
            String id = r.get(oa.OWNER_ID);
            int distance = r.get(oa.ANCESTOR_DISTANCE);
            VersionEvaluationWindow entity = r.into(VersionEvaluationWindow.class);

            closest.merge(
                id,
                new AbstractMap.SimpleEntry<>(distance, entity),
                (a, b) -> a.getKey() <= b.getKey() ? a : b);
          });

      Map<String, VersionEvaluationWindow> result = new HashMap<>();
      closest.forEach((id, entry) -> result.put(id, entry.getValue()));
      return result;
    }
  }
}
