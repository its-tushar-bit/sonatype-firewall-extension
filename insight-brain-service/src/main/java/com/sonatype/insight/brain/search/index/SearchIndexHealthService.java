/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexEstateSnapshotDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexGenerationDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexHealthDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexJobDAO;
import com.sonatype.insight.brain.model.searchindex.SearchIndexEstateSnapshot;
import com.sonatype.insight.brain.model.searchindex.SearchIndexGeneration;
import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJob;

/**
 * Maintains and reads {@link SearchIndexHealth} CURRENT. Queue depth and the oldest-pending pointer
 * are counted from the outbox on refresh rather than tracked per insert, which keeps the shared
 * CURRENT row off the hot write path at the cost of two indexed aggregates per refresh.
 */
@Named
@Singleton
public class SearchIndexHealthService
{
  private final SearchIndexHealthDAO healthDAO;

  private final SearchIndexJobDAO jobDAO;

  private final SearchIndexGenerationDAO generationDAO;

  private final SearchIndexEstateSnapshotDAO estateSnapshotDAO;

  private final SearchIndexChangeDAO changeDAO;

  @Inject
  public SearchIndexHealthService(
      final SearchIndexHealthDAO healthDAO,
      final SearchIndexJobDAO jobDAO,
      final SearchIndexGenerationDAO generationDAO,
      final SearchIndexEstateSnapshotDAO estateSnapshotDAO,
      final SearchIndexChangeDAO changeDAO)
  {
    this.healthDAO = healthDAO;
    this.jobDAO = jobDAO;
    this.generationDAO = generationDAO;
    this.estateSnapshotDAO = estateSnapshotDAO;
    this.changeDAO = changeDAO;
  }

  public SearchIndexHealth getCurrentHealth() {
    return healthDAO.getOrSeedCurrent();
  }

  public SearchIndexEstateSnapshot getCurrentEstate() {
    return estateSnapshotDAO.getCurrent();
  }

  public Optional<SearchIndexGeneration> getServingGeneration() {
    return generationDAO.findByRole(SearchIndexGeneration.ROLE_SERVING);
  }

  public Optional<SearchIndexGeneration> getBuildingGeneration() {
    return generationDAO.findByRole(SearchIndexGeneration.ROLE_BUILDING);
  }

  public Optional<SearchIndexJob> getActiveJob() {
    return jobDAO.findActiveJob();
  }

  /**
   * Records a batch of outbox outcomes, then refreshes derived status once.
   * Call once per indexer batch — never per deleted change.
   */
  public void recordOutboxBatch(final long appliedCount, final long abandonedCount) {
    if (appliedCount <= 0L && abandonedCount <= 0L) {
      return;
    }
    healthDAO.recordAbandonedChanges(abandonedCount);
    refreshDerivedStatus();
  }

  /**
   * Recounts the outbox and rewrites the derived block: queue depth, oldest-pending pointer,
   * health_status, recommended_op and queue_lag. Two indexed aggregates plus an active-job lookup,
   * so it belongs on the batch and Analyze paths, not on anything per-change.
   */
  public void refreshDerivedStatus() {
    SearchIndexHealth health = getCurrentHealth();
    Optional<SearchIndexJob> activeJob = jobDAO.findActiveJob();
    boolean rebuildInProgress = activeJob
        .map(job -> SearchIndexJob.isRebuildType(job.getJobType()))
        .orElse(false);

    long pendingCount = changeDAO.countPending();
    Date oldest = pendingCount > 0L ? changeDAO.findOldestPendingCreatedAt() : null;
    long lagSeconds = oldest == null
        ? 0L
        : Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - oldest.getTime()));

    SearchIndexHealthThresholds.DerivedHealth derived = SearchIndexHealthThresholds.derive(
        lagSeconds,
        pendingCount,
        health.getFailedChangeCount(),
        rebuildInProgress);

    healthDAO.updateDerivedStatus(
        derived.healthStatus(),
        derived.recommendedOp(),
        derived.queueLagSeconds(),
        activeJob.map(SearchIndexJob::getId).orElse(null),
        pendingCount,
        oldest);
  }
}
