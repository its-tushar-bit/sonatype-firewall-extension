/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  /**
   * Look up an active GitHub App by its GitHub-side installation id. Returns {@code null}
   * when no row matches. Used by the relay-link state machine in the admin re-register path
   * to find the row that should record the success/failure transition.
   */
  public GitHubApp getActiveByInstallationId(final Long installationId) {
    if (installationId == null) {
      return null;
    }
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(GITHUB_APP)
          .where(GITHUB_APP.INSTALLATION_ID.eq(installationId)
              .and(GITHUB_APP.IS_ACTIVE.eq(true)))
          .fetchOne());
    }
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

  /**
   * Returns active GitHub Apps whose {@code relay_link_state} is in the supplied set. Used by
   * {@code RelayPollingService} on each cycle to discover Apps that need a relay-registration
   * retry (typically {@code UNREGISTERED} or {@code ERROR}). Apps in {@code OK} or {@code FAILED}
   * are left alone — {@code FAILED} is the slow-sweep's responsibility.
   *
   * <p>
   * Returns an empty list when {@code states} is null or empty.
   */
  public List<GitHubApp> getActiveByRelayLinkState(final Set<String> states) {
    if (states == null || states.isEmpty()) {
      return List.of();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(GITHUB_APP)
          .where(GITHUB_APP.IS_ACTIVE.eq(true)
              .and(GITHUB_APP.RELAY_LINK_STATE.in(states)))
          .fetch()
          .stream()
          .map(this::toEntity)
          .collect(Collectors.toList());
    }
  }

  /**
   * Variant of {@link #getActiveByRelayLinkState(Set)} that further restricts to rows whose
   * {@code last_updated_at} is older than {@code age}. Reserved for the slow-sweep job that
   * promotes {@code FAILED} rows back to {@code ERROR} once the cooldown window has passed;
   * keeping the method here means callers don't need to write their own jOOQ.
   */
  public List<GitHubApp> getActiveByRelayLinkStateOlderThan(final Set<String> states, final Duration age) {
    if (states == null || states.isEmpty() || age == null) {
      return List.of();
    }
    Date cutoff = new Date(System.currentTimeMillis() - age.toMillis());
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(GITHUB_APP)
          .where(GITHUB_APP.IS_ACTIVE.eq(true)
              .and(GITHUB_APP.RELAY_LINK_STATE.in(states))
              .and(GITHUB_APP.LAST_UPDATED_AT.lt(cutoff)))
          .fetch()
          .stream()
          .map(this::toEntity)
          .collect(Collectors.toList());
    }
  }

  /**
   * Bulk-promote rows whose {@code relay_link_state} equals {@code fromState} to {@code toState},
   * resetting {@code relay_link_attempts} to {@code 0}. Used by the hourly slow-sweep to flip
   * {@code FAILED} rows back to {@code ERROR} so the polling-cycle retry loop picks them up
   * again. Returns the number of rows updated.
   */
  public int updateRelayLinkStateBulk(final String fromState, final String toState) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      // IS_ACTIVE filter mirrors resetRelayLinkStateForAllActive: the hourly sweep should only
      // re-queue rows whose tenant/customer is still active. Otherwise the polling-cycle
      // pre-flight wastes cycles re-registering decommissioned apps and may disturb rows for
      // tenants whose license has expired or who have been deleted.
      int updated = tx.dsl()
          .update(GITHUB_APP)
          .set(GITHUB_APP.RELAY_LINK_STATE, toState)
          .set(GITHUB_APP.RELAY_LINK_ATTEMPTS, 0)
          .set(GITHUB_APP.LAST_UPDATED_AT, new Date())
          .where(GITHUB_APP.RELAY_LINK_STATE.eq(fromState))
          .and(GITHUB_APP.IS_ACTIVE.eq(true))
          .execute();
      tx.commit();
      return updated;
    }
  }

  /**
   * Bulk-reset every active GitHub App's {@code relay_link_state} to {@code toState}
   * (and {@code relay_link_attempts} to 0), regardless of current state. Used when the
   * relay registration is dropped at the customer level (e.g. PAT cross-flip): the local
   * App rows still exist but their relay-side mappings are gone, so the link state must
   * reflect the new "not linked" reality. Returns the number of rows updated.
   */
  public int resetRelayLinkStateForAllActive(final String toState) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int updated = tx.dsl()
          .update(GITHUB_APP)
          .set(GITHUB_APP.RELAY_LINK_STATE, toState)
          .set(GITHUB_APP.RELAY_LINK_ATTEMPTS, 0)
          .set(GITHUB_APP.LAST_UPDATED_AT, new Date())
          .where(GITHUB_APP.IS_ACTIVE.eq(true))
          .execute();
      tx.commit();
      return updated;
    }
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
