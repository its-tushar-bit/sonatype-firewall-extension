/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.githubapp.GitHubAppInstallationState;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.Tables.GITHUB_APP_INSTALLATION_STATE;

/**
 * DAO for GitHub App installation state tokens (OAuth + PKCE flow).
 */
@Named
@Singleton
public class GitHubAppInstallationStateDAO
    extends AbstractOperationalSqlDAO<GitHubAppInstallationState>
{
  @Inject
  public GitHubAppInstallationStateDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Atomically finds and deletes the installation state token. This ensures one-time use of state tokens to prevent
   * replay attacks in the OAuth PKCE flow.
   *
   * @param stateToken the state token to find and delete
   * @return the token if found, null otherwise
   */
  public GitHubAppInstallationState findAndDeleteByStateToken(String stateToken) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      GitHubAppInstallationState state = findByStateToken(tx, stateToken);
      if (state != null) {
        delete(tx, state);
      }

      tx.commit();
      return state;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return GITHUB_APP_INSTALLATION_STATE;
  }

  @Override
  public Class<GitHubAppInstallationState> getEntityClass() {
    return GitHubAppInstallationState.class;
  }

  public GitHubAppInstallationState findByStateToken(
      final TransactionContext tx,
      final String stateToken)
  {
    return toEntity(
        tx.dsl()
            .selectFrom(GITHUB_APP_INSTALLATION_STATE)
            .where(GITHUB_APP_INSTALLATION_STATE.STATE_TOKEN.eq(stateToken))
            .fetchOne());
  }

  public void deleteByGitHubAppId(final String githubAppId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByGitHubAppId(tx, githubAppId);
      tx.commit();
    }
  }

  public void deleteByGitHubAppId(final TransactionContext tx, final String githubAppId) {
    tx.dsl()
        .deleteFrom(GITHUB_APP_INSTALLATION_STATE)
        .where(GITHUB_APP_INSTALLATION_STATE.GITHUB_APP_ID.eq(githubAppId))
        .execute();
  }
}
