/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestResult;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlPullRequestResult.SOURCE_CONTROL_PULL_REQUEST_RESULT;

@Named
@Singleton
public class SourceControlPullRequestResultDAO
    extends AbstractOperationalSqlDAO<SourceControlPullRequestResult>
{
  @Inject
  public SourceControlPullRequestResultDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<SourceControlPullRequestResult> getByApplicationId(TransactionContext tx, String applicationId) {
    return tx.dsl()
        .selectFrom(SOURCE_CONTROL_PULL_REQUEST_RESULT)
        .where(SOURCE_CONTROL_PULL_REQUEST_RESULT.APPLICATION_ID.eq(applicationId))
        .fetch(this::toEntity);
  }

  public List<SourceControlPullRequestResult> getByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  public void deleteAll() {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl().deleteFrom(SOURCE_CONTROL_PULL_REQUEST_RESULT).execute();
      tx.commit();
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL_PULL_REQUEST_RESULT;
  }

  @Override
  public Class<SourceControlPullRequestResult> getEntityClass() {
    return SourceControlPullRequestResult.class;
  }
}
