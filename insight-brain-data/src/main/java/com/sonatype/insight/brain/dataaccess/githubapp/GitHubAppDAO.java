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

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.GithubApp.GITHUB_APP;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;

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
  public GitHubAppDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return GITHUB_APP;
  }

  @Override
  public Class<GitHubApp> getEntityClass() {
    return GitHubApp.class;
  }

  public GitHubApp getByOwnerId(final TransactionContext tx, final String ownerId) {
    return toEntity(tx.dsl()
        .selectFrom(GITHUB_APP)
        .where(GITHUB_APP.OWNER_ID.eq(ownerId)
            .and(GITHUB_APP.IS_ACTIVE.eq(true)))
        .fetchOne());
  }

  public GitHubApp getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public Map<String, GitHubApp> getByOwnerIds(final TransactionContext tx, final List<String> ownerIds) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return Map.of();
    }
    return tx.dsl()
        .selectFrom(GITHUB_APP)
        .where(GITHUB_APP.OWNER_ID.in(ownerIds)
            .and(GITHUB_APP.IS_ACTIVE.eq(true)))
        .fetch()
        .stream()
        .map(this::toEntity)
        .collect(Collectors.toMap(GitHubApp::getOwnerId, Function.identity()));
  }

  public Map<String, GitHubApp> getByOwnerIds(final List<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIds(tx, ownerIds);
    }
  }

  public GitHubApp getByOwnerIdNotNull(final TransactionContext tx, final String ownerId) {
    GitHubApp githubApp = getByOwnerId(tx, ownerId);
    if (githubApp == null) {
      throw new NotFoundException("GitHub App not found for ownerId: " + ownerId);
    }
    return githubApp;
  }

  public GitHubApp getByOwnerIdNotNull(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      return getByOwnerIdNotNull(tx, ownerId);
    }
  }

  public GitHubApp getByAppId(final TransactionContext tx, final Integer appId) {
    if (appId == null) {
      throw new DataAccessException("The GitHub App ID cannot be null.");
    }
    return toEntity(tx.dsl()
        .selectFrom(GITHUB_APP)
        .where(GITHUB_APP.APP_ID.eq(appId))
        .fetchOne());
  }

  /**
   * Finds the nearest GitHubApp in the ownership hierarchy for the given owner.
   * Searches up the organization hierarchy starting from the given ownerId and returns
   * the first GitHubApp found, or null if none exists.
   *
   * @param ownerId the owner ID to search from
   * @return the nearest GitHubApp in the hierarchy, or null if not found
   */
  public GitHubApp getNearestGitHubApp(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      return getNearestGitHubApp(tx, ownerId);
    }
  }

  /**
   * Finds the nearest GitHubApp in the ownership hierarchy for the given owner. Uses the OwnerAncestor view to traverse
   * the organization hierarchy.
   *
   * @param tx transaction context
   * @param ownerId the owner ID to search from
   * @return the nearest GitHubApp in the hierarchy, or null if not found
   */
  private GitHubApp getNearestGitHubApp(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .select(GITHUB_APP.fields())
        .from(GITHUB_APP)
        .join(OWNER_ANCESTOR)
        .on(GITHUB_APP.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId)
            .and(GITHUB_APP.IS_ACTIVE.eq(true)))
        .orderBy(OWNER_ANCESTOR.ANCESTOR_DISTANCE)
        .limit(1)
        .fetchOneInto(GitHubApp.class);
  }

  /**
   * Get all GitHub Apps for a given owner (no active filter).
   * Used for UI display to show all available GitHub Apps.
   *
   * @param ownerId the owner ID
   * @return list of all GitHub Apps for the owner
   */
  public List<GitHubApp> getAllByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllByOwnerId(tx, ownerId);
    }
  }

  public List<GitHubApp> getAllByOwnerId(final TransactionContext tx, final String ownerId) {
    return tx.dsl()
        .selectFrom(GITHUB_APP)
        .where(GITHUB_APP.OWNER_ID.eq(ownerId))
        .fetch()
        .stream()
        .map(this::toEntity)
        .collect(Collectors.toList());
  }

  /**
   * Get a GitHub App by its ID.
   *
   * @param githubAppId the GitHub App ID
   * @return the GitHub App, or null if not found
   */
  public GitHubApp getByGithubAppId(final String githubAppId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByGithubAppId(tx, githubAppId);
    }
  }

  public GitHubApp getByGithubAppId(final TransactionContext tx, final String githubAppId) {
    return toEntity(tx.dsl()
        .selectFrom(GITHUB_APP)
        .where(GITHUB_APP.GITHUB_APP_ID.eq(githubAppId))
        .fetchOne());
  }

  /**
   * Activate specific GitHub App and deactivate all others for owner.
   * Self-managed transaction version.
   *
   * @param ownerId the owner ID
   * @param githubAppId the GitHub App ID to activate
   */
  public void activateGitHubApp(final String ownerId, final String githubAppId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      activateGitHubApp(tx, ownerId, githubAppId);
      tx.commit();
    }
  }

  public void activateGitHubApp(final TransactionContext tx, final String ownerId, final String githubAppId) {
    int updated = tx.dsl()
        .update(GITHUB_APP)
        .set(GITHUB_APP.IS_ACTIVE,
            DSL.when(GITHUB_APP.GITHUB_APP_ID.eq(githubAppId), true)
                .otherwise(false))
        .where(GITHUB_APP.OWNER_ID.eq(ownerId))
        .execute();

    if (updated == 0) {
      throw new NotFoundException("GitHub App not found or does not belong to owner: " + githubAppId);
    }
  }

  /**
   * Deactivate all GitHub Apps for owner.
   * Self-managed transaction version.
   *
   * @param ownerId the owner ID
   */
  public void deactivateAllForOwner(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deactivateAllForOwner(tx, ownerId);
      tx.commit();
    }
  }

  public void deactivateAllForOwner(final TransactionContext tx, final String ownerId) {
    tx.dsl()
        .update(GITHUB_APP)
        .set(GITHUB_APP.IS_ACTIVE, false)
        .where(GITHUB_APP.OWNER_ID.eq(ownerId))
        .execute();
  }
}
