/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.evaluation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Lists;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.EvaluationQueue.EVALUATION_QUEUE;

@Named
@Singleton
public class EvaluationQueueDAO
    extends AbstractOperationalSqlDAO<EvaluationQueue>
{
  @Inject
  public EvaluationQueueDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public List<EvaluationQueue> getAll(TransactionContext tx) {
    return tx.dsl()
        .selectFrom(EVALUATION_QUEUE)
        .orderBy(EVALUATION_QUEUE.PRIORITY)
        .fetchInto(EvaluationQueue.class);
  }

  public List<EvaluationQueue> acquireRows(final String workerId, final Integer limit) {
    if (workerId == null) {
      throw new IllegalArgumentException("workerId cannot be null.");
    }
    if (limit != null && limit <= 0) {
      throw new IllegalArgumentException("limit must be positive.");
    }
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      List<EvaluationQueue> acquired;
      if (isDatabaseEmbedded()) {
        acquired = acquireRowsH2(workerId, limit, tx);
      }
      else {
        acquired = acquireRowsPostgres(workerId, limit, tx);
      }
      tx.commit();
      return acquired;
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to acquire evaluation queue rows.", e);
    }
  }

  // For H2, as of writing, we are stuck on 1.4.196
  // H2 is only for a single instance deployment and this version only supports table locks not row locks
  // Queries against a locked table can time out, according to LOCK_TIMEOUT
  // H2 does not support SKIP LOCKED until 2.2.220
  // See https://github.com/h2database/h2database/issues/2985#issuecomment-1703728912
  private List<EvaluationQueue> acquireRowsH2(
      final String workerId,
      final Integer limit,
      final TransactionContext tx) throws SQLException
  {
    var select = tx.dsl()
        .selectFrom(EVALUATION_QUEUE)
        .where(EVALUATION_QUEUE.WORKER_ID.isNull())
        .orderBy(EVALUATION_QUEUE.PRIORITY.asc());

    List<EvaluationQueue> results = (limit != null
        ? select.limit(limit).forUpdate()
        : select.forUpdate())
            .fetchInto(EvaluationQueue.class);

    if (results.isEmpty()) {
      return results;
    }

    Date now = new Date();
    for (List<EvaluationQueue> partition : Lists.partition(results, H2_IN_OPERATOR_THRESHOLD)) {
      tx.dsl()
          .update(EVALUATION_QUEUE)
          .set(EVALUATION_QUEUE.WORKER_ID, workerId)
          .set(EVALUATION_QUEUE.UPDATE_TIME, now)
          .where(EVALUATION_QUEUE.EVALUATION_QUEUE_ID.in(
              partition.stream().map(EvaluationQueue::getId).toList()))
          .execute();
    }

    results.forEach(evaluationQueue -> {
      evaluationQueue.setWorkerId(workerId);
      evaluationQueue.setUpdateTime(now);
    });
    return results;
  }

  private List<EvaluationQueue> acquireRowsPostgres(
      final String workerId,
      final Integer limit,
      final TransactionContext tx)
  {
    Date now = new Date();

    var baseSelect = tx.dsl()
        .select(EVALUATION_QUEUE.EVALUATION_QUEUE_ID)
        .from(EVALUATION_QUEUE)
        .where(EVALUATION_QUEUE.WORKER_ID.isNull())
        .orderBy(EVALUATION_QUEUE.PRIORITY.asc());

    var candidateSelect = (limit != null
        ? baseSelect.limit(limit).forUpdate()
        : baseSelect.forUpdate())
            .skipLocked();

    var updated = DSL.name("updated")
        .as(
            tx.dsl()
                .update(EVALUATION_QUEUE)
                .set(EVALUATION_QUEUE.WORKER_ID, workerId)
                .set(EVALUATION_QUEUE.UPDATE_TIME, now)
                .where(EVALUATION_QUEUE.EVALUATION_QUEUE_ID.in(candidateSelect))
                .returning());

    return tx.dsl()
        .with(updated)
        .selectFrom(updated)
        .orderBy(updated.field(EVALUATION_QUEUE.PRIORITY).asc())
        .fetchInto(EvaluationQueue.class);
  }

  public void unacquireRows(final Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return;
    }
    Date now = new Date();
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      for (List<String> partition : Lists.partition(new ArrayList<>(ids), getInOperatorThreshold())) {
        tx.dsl()
            .update(EVALUATION_QUEUE)
            .set(EVALUATION_QUEUE.WORKER_ID, (String) null)
            .set(EVALUATION_QUEUE.UPDATE_TIME, now)
            .where(EVALUATION_QUEUE.EVALUATION_QUEUE_ID.in(partition))
            .execute();
      }
      tx.commit();
    }
  }

  public void clearInactiveWorkerIds(final Set<String> activeWorkerIds) {
    Date now = new Date();
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      var update = tx.dsl()
          .update(EVALUATION_QUEUE)
          .set(EVALUATION_QUEUE.WORKER_ID, (String) null)
          .set(EVALUATION_QUEUE.UPDATE_TIME, now)
          .where(EVALUATION_QUEUE.WORKER_ID.isNotNull());

      if (CollectionUtils.isNotEmpty(activeWorkerIds)) {
        update = update.and(EVALUATION_QUEUE.WORKER_ID.notIn(activeWorkerIds));
      }

      update.execute();
      tx.commit();
    }
  }

  public void clearExpiredWorkerIds(final long expirationMillis) {
    if (expirationMillis <= 0) {
      throw new IllegalArgumentException("expirationMillis must be positive.");
    }
    Date now = new Date();
    Date expirationThreshold = new Date(System.currentTimeMillis() - expirationMillis);

    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      var expiredWorkerIds = tx.dsl()
          .select(EVALUATION_QUEUE.WORKER_ID)
          .from(EVALUATION_QUEUE)
          .where(EVALUATION_QUEUE.WORKER_ID.isNotNull())
          .groupBy(EVALUATION_QUEUE.WORKER_ID)
          .having(DSL.max(EVALUATION_QUEUE.UPDATE_TIME).lt(expirationThreshold));

      tx.dsl()
          .update(EVALUATION_QUEUE)
          .set(EVALUATION_QUEUE.WORKER_ID, (String) null)
          .set(EVALUATION_QUEUE.UPDATE_TIME, now)
          .where(EVALUATION_QUEUE.WORKER_ID.isNotNull())
          .and(EVALUATION_QUEUE.WORKER_ID.in(expiredWorkerIds))
          .execute();
      tx.commit();
    }
  }

  public Integer getMaxPriority() {
    try (TransactionContext tx = createTransactionContext()) {
      return getMaxPriority(tx);
    }
  }

  public Integer getMaxPriority(final TransactionContext tx) {
    return tx.dsl()
        .select(DSL.max(EVALUATION_QUEUE.PRIORITY))
        .from(EVALUATION_QUEUE)
        .fetchOne(0, Integer.class);
  }

  public void deleteAll(final TransactionContext tx) {
    tx.dsl()
        .deleteFrom(EVALUATION_QUEUE)
        .execute();
  }

  public Set<ThirdPartySbomMetadata> findExisting(final Collection<ThirdPartySbomMetadata> sboms) {
    try (TransactionContext tx = createTransactionContext()) {
      return findExisting(tx, sboms);
    }
  }

  @SuppressWarnings("unchecked")
  public Set<ThirdPartySbomMetadata> findExisting(
      final TransactionContext tx,
      final Collection<ThirdPartySbomMetadata> sboms)
  {
    if (CollectionUtils.isEmpty(sboms)) {
      return Set.of();
    }

    SQLDialect dialect = tx.dsl().dialect();

    Condition condition;
    if (dialect == SQLDialect.H2) {
      condition = sboms.stream()
          .map(sbom -> EVALUATION_QUEUE.APPLICATION_ID.eq(sbom.getApplicationId())
              .and(EVALUATION_QUEUE.VERSION.eq(sbom.getSbomVersion()))
              .and(EVALUATION_QUEUE.STAGE_TYPE_ID.eq(ComplianceStageType.ID)))
          .reduce(DSL.falseCondition(), Condition::or);
    }
    else {
      Row2<String, String>[] rows = sboms.stream()
          .map(sbom -> DSL.row(sbom.getApplicationId(), sbom.getSbomVersion()))
          .toArray(Row2[]::new);
      condition = DSL.row(EVALUATION_QUEUE.APPLICATION_ID, EVALUATION_QUEUE.VERSION)
          .in(rows)
          .and(EVALUATION_QUEUE.STAGE_TYPE_ID.eq(ComplianceStageType.ID));
    }

    Set<Record2<String, String>> existingKeys = new HashSet<>(tx.dsl()
        .select(EVALUATION_QUEUE.APPLICATION_ID, EVALUATION_QUEUE.VERSION)
        .from(EVALUATION_QUEUE)
        .where(condition)
        .fetch());

    return sboms.stream()
        .filter(sbom -> existingKeys.contains(
            tx.dsl()
                .newRecord(EVALUATION_QUEUE.APPLICATION_ID, EVALUATION_QUEUE.VERSION)
                .values(sbom.getApplicationId(), sbom.getSbomVersion())))
        .collect(Collectors.toSet());
  }

  @Override
  public Table<?> getJooqTable() {
    return EVALUATION_QUEUE;
  }

  @Override
  public Class<EvaluationQueue> getEntityClass() {
    return EvaluationQueue.class;
  }
}
