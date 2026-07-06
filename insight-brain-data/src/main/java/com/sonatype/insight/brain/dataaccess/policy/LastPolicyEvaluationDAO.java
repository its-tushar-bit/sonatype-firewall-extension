/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.policy.LastPolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.LastPolicyEvaluation.LAST_POLICY_EVALUATION;

/**
 * @since 1.12
 */
@Named
@Singleton
public class LastPolicyEvaluationDAO
    extends AbstractOperationalSqlDAO<LastPolicyEvaluation>
{
  @Inject
  public LastPolicyEvaluationDAO(
      final OperationalDataStore operationalDataStore,
      final SearchIndexManager searchIndexManager)
  {
    super(operationalDataStore, searchIndexManager);
  }

  @Override
  public int insert(final TransactionContext tx, final LastPolicyEvaluation entity) {
    return super.insert(tx, entity);
  }

  public void insertIfPossibleLastPolicyEvaluation(
      final TransactionContext tx,
      final PolicyEvaluation newestPolicyEvaluation)
  {
    if (newestPolicyEvaluation != null) {
      insert(tx, new LastPolicyEvaluation(newestPolicyEvaluation.getId(), newestPolicyEvaluation.getApplicationId(),
          newestPolicyEvaluation.getStageTypeId()));
    }
  }

  public LastPolicyEvaluation getByApplicationIdAndStageTypeId(final String applicationId, final String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(LAST_POLICY_EVALUATION)
          .where(LAST_POLICY_EVALUATION.APPLICATION_ID.eq(applicationId))
          .and(LAST_POLICY_EVALUATION.STAGE_TYPE_ID.eq(stageTypeId))
          .fetchOne());
    }
  }

  @Override
  public int update(TransactionContext tx, LastPolicyEvaluation entity) {
    throw new UnsupportedOperationException("The LastPolicyEvaluation table does not support update operations");
  }

  @Override
  protected SearchIndexChange newSearchIndexChangeForInsert(LastPolicyEvaluation entity) {
    if (ProxyStageType.ID.equals(entity.getStageTypeId())) {
      return null;
    }
    return new SearchIndexChange(ChangeType.LAST_POLICY_EVALUATION,
        entity.getApplicationId() + ':' + entity.getStageTypeId());
  }

  @Override
  public Table<?> getJooqTable() {
    return LAST_POLICY_EVALUATION;
  }

  @Override
  public Class<LastPolicyEvaluation> getEntityClass() {
    return LastPolicyEvaluation.class;
  }
}
