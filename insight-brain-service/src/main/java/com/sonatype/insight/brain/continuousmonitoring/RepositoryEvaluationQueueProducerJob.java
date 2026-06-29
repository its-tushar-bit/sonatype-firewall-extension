/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringHostedRepoItemDAO;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringQueueItemDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ContinuousMonitoringHostedRepoItem.CONTINUOUS_MONITORING_HOSTED_REPO_ITEM;

/**
 * Continuous monitoring producer for the Hosted Repo flow (CLM-40039 Section 6.1). Runs once per
 * Quartz fire and pages through monitoring-enabled hosted-repo components, enqueueing them as
 * parent + satellite rows by orchestrating {@link ContinuousMonitoringQueueItemDAO} and
 * {@link ContinuousMonitoringHostedRepoItemDAO} inside a single transaction (insert parents,
 * insert satellites with ignore-duplicate-key, delete orphan parents whose satellite was deduped
 * on the natural-key UNIQUE). The consumer drains rows in strict FIFO order by
 * {@code create_time ASC}, so the producer no longer assigns per-row priorities — every parent
 * row is written with {@link ContinuousMonitoringQueueItem#DEFAULT_PRIORITY}. The natural-key UNIQUE
 * on the satellite table guarantees a component already PENDING or IN_PROGRESS is silently
 * deduped on the next cycle, so its place in line is preserved.
 * <p>
 * <strong>Ordering semantics:</strong> Every row inserted during a single producer cycle shares
 * the same {@code cycleStart} timestamp, so within one cycle the consumer's
 * {@code ORDER BY create_time ASC, id ASC} falls through to the ID tiebreaker — within-cycle
 * ordering is therefore deterministic but unrelated to the eligibility selector's emission order.
 * Across cycles, an older cycle's PENDING rows always drain before any row from a newer cycle.
 * This matches the contract that the age of an item in the queue is the only thing that affects
 * its position.
 * Gated on {@link SystemConfigurationPropertyFeature#HOSTED_REPOSITORY_EVALUATION}.
 * <p>
 * <strong>Admin Task Endpoint Rename:</strong> The legacy {@code HostedRepositoryMonitorScheduler}
 * exposed {@code POST /tasks/triggerHostedRepositoryMonitor}. This job exposes
 * {@code POST /tasks/RepositoryEvaluationQueueProducerJob} (derived from {@link #NAME}).
 * Any operator runbooks, monitoring scripts, or internal docs referencing the old endpoint
 * must be updated post-upgrade.
 */
@Named
@Singleton
public class RepositoryEvaluationQueueProducerJob
    extends AbstractContinuousMonitoringProducerJob<RepositoryComponent>
{
  public static final String NAME = "RepositoryEvaluationQueueProducerJob";

  static final int DEFAULT_ELIGIBILITY_PAGE_SIZE = 1000;

  private final HostedRepoEligibilitySelector eligibilitySelector;

  private final ContinuousMonitoringQueueItemDAO queueItemDAO;

  private final ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO;

  @Inject
  public RepositoryEvaluationQueueProducerJob(
      final HostedRepoEligibilitySelector eligibilitySelector,
      final ContinuousMonitoringQueueItemDAO queueItemDAO,
      final ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO)
  {
    super(NAME);
    this.eligibilitySelector = eligibilitySelector;
    this.queueItemDAO = queueItemDAO;
    this.hostedRepoItemDAO = hostedRepoItemDAO;
  }

  @Override
  protected EligibilitySelector<RepositoryComponent> getEligibilitySelector() {
    return eligibilitySelector;
  }

  /**
   * Hosted-repo flow uses a fixed page size of {@value #DEFAULT_ELIGIBILITY_PAGE_SIZE}; the
   * abstract base's "operator-tunable" wording on {@code getEligibilityPageSize()} is the contract
   * for future flows that wire their own {@code SystemConfigurationProperty}. There is no
   * operator demand for live page-size tuning on hosted-repo today; if a future need surfaces,
   * a new property can be wired without changing the framework contract.
   */
  @Override
  protected int getEligibilityPageSize() {
    return DEFAULT_ELIGIBILITY_PAGE_SIZE;
  }

  @Override
  protected boolean isEnabled() {
    return SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled();
  }

  @Override
  protected int enqueueBatch(final List<RepositoryComponent> candidates, final Instant cycleStart) {
    List<ContinuousMonitoringQueueItem> parents = new ArrayList<>(candidates.size());
    List<ContinuousMonitoringHostedRepoItem> satellites = new ArrayList<>(candidates.size());
    List<String> parentIds = new ArrayList<>(candidates.size());
    for (int i = 0; i < candidates.size(); i++) {
      RepositoryComponent rc = candidates.get(i);
      String queueId = UUID.randomUUID().toString();
      parents.add(new ContinuousMonitoringQueueItem(
          queueId,
          ContinuousMonitoringFlowType.HOSTED_REPO,
          ContinuousMonitoringQueueItem.DEFAULT_PRIORITY,
          Date.from(cycleStart)));
      satellites.add(new ContinuousMonitoringHostedRepoItem(
          queueId,
          rc.getRepositoryId(),
          rc.getHash()));
      parentIds.add(queueId);
    }
    // Producer-side orchestration across the queue + satellite DAOs in a single transaction:
    // insert parents → insert satellites with ignore-duplicate-key on the natural-key UNIQUE
    // → delete any orphan parents whose satellite was deduped (concurrent producer race).
    try (TransactionContext tx = queueItemDAO.createTransactionContext()) {
      tx.begin();
      queueItemDAO.insertBatch(tx, parents, false);
      hostedRepoItemDAO.insertIgnoreDuplicateKey(tx, satellites);
      queueItemDAO.deleteOrphanParentsForSatelliteTable(
          tx,
          parentIds,
          CONTINUOUS_MONITORING_HOSTED_REPO_ITEM.QUEUE_ID);
      tx.commit();
    }
    // Returns the pre-dedup candidate count rather than the post-dedup survivors. This is a
    // deliberate v1 simplification: under @DisallowConcurrentExecution the orphan-deletion path
    // only fires in the rare cross-tenant / cross-node race where two producers enqueue the same
    // (repository_id, component_hash) within one tick — practically zero in the daily Quartz
    // schedule. Returning parents.size() keeps the call site simple; the cycle log overstates by
    // at most the orphan count when it does fire (visible in the queueItemDAO orphan-delete log).
    // If a future flow needs the exact post-dedup count, deleteOrphanParentsForSatelliteTable
    // would need to return an int and this would become parents.size() - orphansDeleted.
    return parents.size();
  }

  @Override
  protected String getFlowLogTag() {
    return "hosted_repo";
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
