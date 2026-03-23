/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.ide;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ide.UserIdePolicyEvaluation;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.UserIdePolicyEvaluation.USER_IDE_POLICY_EVALUATION;

@Named
@Singleton
public class UserIdePolicyEvaluationDAO
    extends AbstractOperationalSqlDAO<UserIdePolicyEvaluation>
{
  @Inject
  public UserIdePolicyEvaluationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public long getCountSince(Date sinceUtcDate) {
    try (TransactionContext tx = createTransactionContext()) {
      Long count = tx.dsl()
          .selectCount()
          .from(USER_IDE_POLICY_EVALUATION)
          .where(USER_IDE_POLICY_EVALUATION.LAST_EVALUATION_TIME.ge(sinceUtcDate))
          .fetchOne(0, Long.class);
      return count != null ? count : 0L;
    }
  }

  public UserIdePolicyEvaluation getByUsername(String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsername(tx, username);
    }
  }

  public UserIdePolicyEvaluation getByUsername(TransactionContext tx, String username) {
    if (username == null || username.trim().isEmpty()) {
      throw new DataAccessException("The username name cannot be null or empty.");
    }
    return toEntity(tx.dsl()
        .selectFrom(USER_IDE_POLICY_EVALUATION)
        .where(USER_IDE_POLICY_EVALUATION.USERNAME.eq(username))
        .fetchOne());
  }

  public void upsert(String username) {
    UserIdePolicyEvaluation entity = getByUsername(username);
    if (entity != null) {
      entity.setLastEvaluationTime(new Date());
      update(entity);
    }
    else {
      insert(new UserIdePolicyEvaluation(username, new Date()));
    }
  }

  public void deleteByUsername(TransactionContext tx, String username) {
    UserIdePolicyEvaluation entity = getByUsername(tx, username);
    if (entity != null) {
      delete(tx, entity);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return USER_IDE_POLICY_EVALUATION;
  }

  @Override
  public Class<UserIdePolicyEvaluation> getEntityClass() {
    return UserIdePolicyEvaluation.class;
  }
}
