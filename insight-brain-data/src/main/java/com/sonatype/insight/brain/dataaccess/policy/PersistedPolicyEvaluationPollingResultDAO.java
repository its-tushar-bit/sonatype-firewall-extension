/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PersistedPolicyEvaluationPollingResult.PERSISTED_POLICY_EVALUATION_POLLING_RESULT;

@Named
@Singleton
public class PersistedPolicyEvaluationPollingResultDAO
    extends AbstractOperationalSqlDAO<PersistedPolicyEvaluationPollingResult>
{
  @Inject
  public PersistedPolicyEvaluationPollingResultDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  protected UpdatableRecord<?> fromEntity(
      final UpdatableRecord<?> record,
      final PersistedPolicyEvaluationPollingResult entity)
  {
    super.fromEntity(record, entity);
    record.set(PERSISTED_POLICY_EVALUATION_POLLING_RESULT.POLICY_EVALUATION_POLLING_RESULT_JSON,
        entity.getPolicyEvaluationPollingResult() != null
            ? JsonUtils.writeUnformatted(entity.getPolicyEvaluationPollingResult())
            : null);
    return record;
  }

  public PersistedPolicyEvaluationPollingResult getByApplicationIdAndStatusId(String applicationId, String statusId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(PERSISTED_POLICY_EVALUATION_POLLING_RESULT)
          .where(PERSISTED_POLICY_EVALUATION_POLLING_RESULT.APPLICATION_ID.eq(applicationId))
          .and(PERSISTED_POLICY_EVALUATION_POLLING_RESULT.STATUS_ID.eq(statusId))
          .fetchOne());
    }
  }

  public void deleteBeforeOrOn(Date date) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .deleteFrom(PERSISTED_POLICY_EVALUATION_POLLING_RESULT)
          .where(PERSISTED_POLICY_EVALUATION_POLLING_RESULT.CREATE_TIME.le(date))
          .execute();
      tx.commit();
    }
  }

  public void deleteAll() {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .deleteFrom(PERSISTED_POLICY_EVALUATION_POLLING_RESULT)
          .execute();
      tx.commit();
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return PERSISTED_POLICY_EVALUATION_POLLING_RESULT;
  }

  @Override
  public Class<PersistedPolicyEvaluationPollingResult> getEntityClass() {
    return PersistedPolicyEvaluationPollingResult.class;
  }
}
