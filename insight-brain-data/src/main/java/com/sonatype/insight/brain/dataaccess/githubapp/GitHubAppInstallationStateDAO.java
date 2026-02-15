/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.githubapp.GitHubAppInstallationState;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * DAO for GitHub App installation state tokens (OAuth + PKCE flow).
 */
@Named
@Singleton
public class GitHubAppInstallationStateDAO extends AbstractOperationalSqlDAO<GitHubAppInstallationState>
{
  @Inject
  public GitHubAppInstallationStateDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Atomically finds and deletes the installation state token.
   * This ensures one-time use of state tokens to prevent replay attacks in the OAuth PKCE flow.
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
  public void insert(GitHubAppInstallationState installationState) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      insert(tx, installationState);
      tx.commit();
    }
  }

  public GitHubAppInstallationState findByStateToken(
      TransactionContext tx,
      String stateToken)
  {
    String query = "SELECT e FROM GitHubAppInstallationState e WHERE e.stateToken=?1";
    return get(tx, query, stateToken);
  }
}
