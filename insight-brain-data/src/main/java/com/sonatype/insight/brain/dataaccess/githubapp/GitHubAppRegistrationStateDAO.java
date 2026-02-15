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
import com.sonatype.insight.brain.model.githubapp.GitHubAppRegistrationState;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * DAO for GitHub App registration state tokens (manifest flow).
 */
@Named
@Singleton
public class GitHubAppRegistrationStateDAO extends AbstractOperationalSqlDAO<GitHubAppRegistrationState>
{
  @Inject
  public GitHubAppRegistrationStateDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Atomically finds and deletes the registration state token.
   * This ensures one-time use of state tokens to prevent replay attacks in the manifest registration flow.
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
  public void insert(GitHubAppRegistrationState registrationState) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      insert(tx, registrationState);
      tx.commit();
    }
  }

  public GitHubAppRegistrationState findByStateToken(
      TransactionContext tx,
      String stateToken)
  {
    String query = "SELECT e FROM GitHubAppRegistrationState e WHERE e.stateToken=?1";
    return get(tx, query, stateToken);
  }
}
