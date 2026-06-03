/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.relay.RelayEventLogDAO;
import com.sonatype.insight.brain.model.Application;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters relay events that have already been processed. Two checks:
 * <ol>
 * <li>Primary: the {@code eventId} (relay UUID) was seen before. Cheap, runs before mapping.</li>
 * <li>Secondary: the {@code (applicationPublicId, pullRequestNumber, commitHash, mode, eventType)}
 * tuple was seen before. Runs after mapping; covers the cutover window where a logically-equivalent
 * event may arrive with a different UUID. {@code mode} ("pat" / "github-app") discriminates rows
 * produced under different relay registration kinds so a customer migrating between modes does
 * not have new-mode events suppressed by stale rows from the old mode. {@code eventType}
 * discriminates distinct logical events on the same tuple (e.g. close + reopen of a PR with no
 * new commits — both produce the same (app, pr, commit, mode) but distinct event types).</li>
 * </ol>
 *
 * <p>
 * {@link #recordProcessed} is the atomic gate. The primary {@link #isPrimaryDuplicate} read
 * lets the poller skip mapping cheaply on redeliveries; concurrent writers still collapse on
 * the {@code event_id} unique constraint inside the DAO.
 */
@Named
@Singleton
public class RelayEventDeduplicator
{
  private static final Logger log = LoggerFactory.getLogger(RelayEventDeduplicator.class);

  private final RelayEventLogDAO relayEventLogDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public RelayEventDeduplicator(
      final RelayEventLogDAO relayEventLogDAO,
      final ApplicationDAO applicationDAO)
  {
    this.relayEventLogDAO = relayEventLogDAO;
    this.applicationDAO = applicationDAO;
  }

  /**
   * Resolves an internal application id to its public id, or returns {@code null} when the
   * application cannot be loaded.
   */
  public String resolveApplicationPublicId(final String applicationId) {
    if (StringUtils.isBlank(applicationId)) {
      return null;
    }
    Application application = applicationDAO.getById(applicationId);
    return application != null ? application.getPublicId() : null;
  }

  /**
   * Returns true if {@code eventId} has already been recorded.
   */
  public boolean isPrimaryDuplicate(final String eventId) {
    return !StringUtils.isBlank(eventId) && relayEventLogDAO.existsByEventId(eventId);
  }

  /**
   * Returns true if the secondary key has been seen before under the given {@code mode} and
   * {@code eventType}. A null {@code applicationPublicId} disables the check (the secondary
   * key needs an application context to be meaningful). {@code mode} ensures that rows
   * recorded under a different relay registration kind do not over-match a fresh event
   * arriving in the current mode; {@code eventType} ensures that logically-distinct events
   * sharing a (app, pr, commit) tuple — most notably close+reopen of the same PR on the
   * same head SHA — are not collapsed.
   */
  public boolean isSecondaryDuplicate(
      final String applicationPublicId,
      final Integer pullRequestNumber,
      final String commitHash,
      final String mode,
      final String eventType)
  {
    return relayEventLogDAO.isDuplicateBySecondaryKey(
        applicationPublicId, pullRequestNumber, commitHash, mode, eventType);
  }

  /**
   * Atomically records that an event was processed. Returns {@code true} if a new row was
   * written, {@code false} if a row already existed for {@code eventId}. A null/blank
   * {@code eventId} is treated as new but no row is written (nothing to dedup against).
   *
   * <p>
   * Late-arriving-app caveat: when an event maps to zero applications (no matching
   * repository URL), or every mapped app's secondary key is already a duplicate, this
   * method is still called with null secondary-key fields so primary dedup blocks
   * redelivery of the same {@code eventId}. If an application is later added that matches
   * that repo URL, the next genuine event for that app arriving via the relay with the
   * same {@code eventId} will be a primary duplicate and silently dropped. The window for
   * such a missed event is bounded by relay redelivery (15-min visibility timeout). Any
   * re-trigger for the missed window comes from the SCM polling fallback or a manual
   * scan trigger.
   *
   * <p>
   * Fan-out caveat: when one relay event maps to multiple
   * {@link com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent}s
   * (multiple applications bound to the same repository URL), only the first application's
   * secondary key is recorded; the {@code event_id} unique constraint blocks per-application
   * rows. Apps 2..N rely on primary dedup alone during the relay/legacy cutover. Tightening
   * this would require a per-(event, app) row schema; the current design is sufficient for
   * the typical 1-app-per-repo case.
   */
  public boolean recordProcessed(
      final String eventId,
      final String applicationPublicId,
      final Integer pullRequestNumber,
      final String commitHash,
      final String eventType,
      final String mode)
  {
    if (StringUtils.isBlank(eventId)) {
      return true;
    }
    boolean inserted = relayEventLogDAO.recordIfNew(eventId, applicationPublicId, pullRequestNumber, commitHash,
        eventType, mode);
    if (!inserted) {
      log.debug("Relay eventId={} already recorded; skipping", eventId);
    }
    return inserted;
  }
}
