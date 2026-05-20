/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
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

  public List<GitHubApp> getByOwnerId(final TransactionContext tx, final String ownerId) {
    return tx.dsl()
        .selectFrom(GITHUB_APP)
        .where(GITHUB_APP.OWNER_ID.eq(ownerId)
            .and(GITHUB_APP.IS_ACTIVE.eq(true)))
        .fetch()
        .stream()
        .map(this::toEntity)
        .collect(Collectors.toList());
  }

  public List<GitHubApp> getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  /**
   * Get all installed GitHub Apps for a given owner regardless of active status.
   * "Installed" means the app has a non-null installation ID (completed the GitHub installation flow).
   * Used by the composite source control endpoint so the frontend knows reactivatable apps exist.
   */
  public List<GitHubApp> getInstalledByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(GITHUB_APP)
          .where(GITHUB_APP.OWNER_ID.eq(ownerId)
              .and(GITHUB_APP.INSTALLATION_ID.isNotNull()))
          .fetch()
          .stream()
          .map(this::toEntity)
          .collect(Collectors.toList());
    }
  }

  public Map<String, List<GitHubApp>> getAllByOwnerIds(final List<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllByOwnerIds(tx, ownerIds);
    }
  }

  public Map<String, List<GitHubApp>> getAllByOwnerIds(final TransactionContext tx, final List<String> ownerIds) {
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
        .collect(Collectors.groupingBy(GitHubApp::getOwnerId));
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

  public List<GitHubApp> getNearestGitHubApps(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      var minDistance = tx.dsl()
          .select(DSL.min(OWNER_ANCESTOR.ANCESTOR_DISTANCE))
          .from(OWNER_ANCESTOR)
          .join(GITHUB_APP)
          .on(GITHUB_APP.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
          .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
          .and(GITHUB_APP.IS_ACTIVE.eq(true));

      return tx.dsl()
          .select(GITHUB_APP.fields())
          .from(GITHUB_APP)
          .join(OWNER_ANCESTOR)
          .on(GITHUB_APP.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
          .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
          .and(GITHUB_APP.IS_ACTIVE.eq(true))
          .and(OWNER_ANCESTOR.ANCESTOR_DISTANCE.eq(minDistance))
          .orderBy(GITHUB_APP.GITHUB_APP_ID.asc())
          .fetchInto(GitHubApp.class);
    }
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
        .set(GITHUB_APP.LAST_UPDATED_AT, new Date())
        .where(GITHUB_APP.OWNER_ID.eq(ownerId))
        .execute();
  }

  public void activateInstalledForOwner(final TransactionContext tx, final String ownerId) {
    tx.dsl()
        .update(GITHUB_APP)
        .set(GITHUB_APP.IS_ACTIVE, true)
        .set(GITHUB_APP.LAST_UPDATED_AT, new Date())
        .where(GITHUB_APP.OWNER_ID.eq(ownerId)
            .and(GITHUB_APP.INSTALLATION_ID.isNotNull()))
        .execute();
  }

  public List<GitHubApp> findInactive() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(GITHUB_APP)
          .where(GITHUB_APP.IS_ACTIVE.eq(false))
          .fetch()
          .stream()
          .map(this::toEntity)
          .collect(Collectors.toList());
    }
  }
}
