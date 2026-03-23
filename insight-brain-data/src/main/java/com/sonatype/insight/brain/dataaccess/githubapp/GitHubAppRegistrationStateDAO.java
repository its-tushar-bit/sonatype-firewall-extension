/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.githubapp.GitHubAppRegistrationState;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.Tables.GITHUB_APP_REGISTRATION_STATE;

/**
 * DAO for GitHub App registration state tokens (manifest flow).
 */
@Named
@Singleton
public class GitHubAppRegistrationStateDAO
    extends AbstractOperationalSqlDAO<GitHubAppRegistrationState>
{
  @Inject
  public GitHubAppRegistrationStateDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Atomically finds and deletes the registration state token. This ensures one-time use of state tokens to prevent
   * replay attacks in the manifest registration flow.
   *
   * @param stateToken the state token to find and delete
   * @return the token if found, null otherwise
   */
  public GitHubAppRegistrationState findAndDeleteByStateToken(String stateToken) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      GitHubAppRegistrationState state = findByStateToken(tx, stateToken);
      if (state != null) {
        delete(tx, state);
      }

      tx.commit();
      return state;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return GITHUB_APP_REGISTRATION_STATE;
  }

  @Override
  public Class<GitHubAppRegistrationState> getEntityClass() {
    return GitHubAppRegistrationState.class;
  }

  public GitHubAppRegistrationState findByStateToken(
      final TransactionContext tx,
      final String stateToken)
  {
    return toEntity(
        tx.dsl()
            .selectFrom(GITHUB_APP_REGISTRATION_STATE)
            .where(GITHUB_APP_REGISTRATION_STATE.STATE_TOKEN.eq(stateToken))
            .fetchOne());
  }
}
