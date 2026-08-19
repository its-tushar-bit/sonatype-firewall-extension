/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Iterables;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ReevaluateCascadeRequest.REEVALUATE_CASCADE_REQUEST;

/**
 * Data access object for managing cascade re-evaluation requests.
 *
 * @since 1.196
 */
@Named
@Singleton
public class ReevaluateCascadeRequestDAO
    extends AbstractOperationalSqlDAO<ReevaluateCascadeRequest>
{
  private final ReevaluateCascadeProgressDAO reevaluateCascadeProgressDAO;

  @Inject
  public ReevaluateCascadeRequestDAO(
      final OperationalDataStore operationalDataStore,
      final ReevaluateCascadeProgressDAO reevaluateCascadeProgressDAO)
  {
    super(operationalDataStore);
    this.reevaluateCascadeProgressDAO = reevaluateCascadeProgressDAO;
  }

  public List<ReevaluateCascadeRequest> getByComponentHash(final String componentHash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByComponentHash(tx, componentHash);
    }
  }

  public List<ReevaluateCascadeRequest> getByComponentHash(final TransactionContext tx, final String componentHash) {
    return tx.dsl()
        .selectFrom(REEVALUATE_CASCADE_REQUEST)
        .where(REEVALUATE_CASCADE_REQUEST.COMPONENT_REFERENCE_HASH.eq(componentHash))
        .fetch(super::toEntity);
  }

  public List<ReevaluateCascadeRequest> findBeforeOrOn(final TransactionContext tx, Date date) {
    return tx.dsl()
        .selectFrom(REEVALUATE_CASCADE_REQUEST)
        .where(REEVALUATE_CASCADE_REQUEST.CREATED_AT.le(date))
        .fetch(super::toEntity);
  }

  public void deleteByRequestIds(TransactionContext tx, Set<String> requestIds) {
    if (requestIds.isEmpty()) {
      return;
    }

    // Delete children first (progress entries)
    reevaluateCascadeProgressDAO.deleteByRequestIds(tx, requestIds);

    // Then delete parents (requests)
    Iterable<List<String>> batches = Iterables.partition(requestIds, getInOperatorThreshold());

    for (List<String> batch : batches) {
      tx.dsl()
          .deleteFrom(REEVALUATE_CASCADE_REQUEST)
          .where(REEVALUATE_CASCADE_REQUEST.REEVALUATE_CASCADE_REQUEST_ID.in(batch))
          .execute();
    }
  }

  @Override
  public void delete(TransactionContext tx, ReevaluateCascadeRequest entity) {
    tx.dsl()
        .deleteFrom(REEVALUATE_CASCADE_REQUEST)
        .where(REEVALUATE_CASCADE_REQUEST.REEVALUATE_CASCADE_REQUEST_ID.eq(entity.getId()))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return REEVALUATE_CASCADE_REQUEST;
  }

  @Override
  public Class<ReevaluateCascadeRequest> getEntityClass() {
    return ReevaluateCascadeRequest.class;
  }
}
