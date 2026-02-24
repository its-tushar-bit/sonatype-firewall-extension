/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.201
 */
@Named
@Singleton
public class GitHubAppDAO
    extends AbstractOperationalSqlDAO<GitHubApp>
    implements RotatableSecrets
{
  @Inject
  public GitHubAppDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public GitHubApp getByOwnerId(TransactionContext tx, String ownerId) {
    String query = "SELECT entity FROM GitHubApp entity WHERE entity.ownerId=?1";
    return get(tx, query, ownerId);
  }

  public GitHubApp getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      return getByOwnerId(tx, ownerId);
    }
  }

  public Map<String, GitHubApp> getByOwnerIds(TransactionContext tx, List<String> ownerIds) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return Map.of();
    }
    String query = "SELECT entity FROM GitHubApp entity WHERE entity.ownerId IN (?1)";
    List<GitHubApp> gitHubApps = createQuery(tx, query, ownerIds).getResultList();
    return gitHubApps.stream().collect(Collectors.toMap(GitHubApp::getOwnerId, Function.identity()));
  }

  public Map<String, GitHubApp> getByOwnerIds(List<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      return getByOwnerIds(tx, ownerIds);
    }
  }

  public GitHubApp getByOwnerIdNotNull(TransactionContext tx, String ownerId) {
    GitHubApp githubApp = getByOwnerId(tx, ownerId);
    if (githubApp == null) {
      throw new NotFoundException("GitHub App not found for ownerId: " + ownerId);
    }
    return githubApp;
  }

  public GitHubApp getByOwnerIdNotNull(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      return getByOwnerIdNotNull(tx, ownerId);
    }
  }

  public GitHubApp getByAppId(TransactionContext tx, Integer appId) {
    if (appId == null) {
      throw new DataAccessException("The GitHub App ID cannot be null.");
    }
    String query = "SELECT entity FROM GitHubApp entity WHERE entity.appId = ?1";
    return get(tx, query, appId);
  }

  public void updateGitHubApp(GitHubApp gitHubApp) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      update(tx, gitHubApp);
      tx.commit();
    }
  }
}
