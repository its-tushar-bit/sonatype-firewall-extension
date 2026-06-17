/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.util.List;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringHostedRepoItemDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ContinuousMonitoringFlowProcessor} for the Hosted Repo flow (CLM-40039 Section 6.3).
 * Resolves the satellite row to (repositoryId, componentHash), re-asserts that the repository is
 * a monitoring-enabled hosted repository owned by the current tenant (defense in depth on the
 * shared executor surface — Section 4.3 / STRIDE Section 9), then re-evaluates the component
 * via {@link RepositoryPolicyEvaluator#evaluateForMonitoring}. The evaluator updates
 * {@code last_evaluation_time} as part of evaluation.
 * <p>
 * Note: This processor does NOT call {@code stampComponentId} on the
 * {@code repository_policy_violation} table. That method is specific to NXRM-hosted repositories
 * where an external NXRM component ID needs to be stamped onto violations for correlation with the
 * NXRM database. For pure IQ Server hosted repositories (the use case here), the
 * {@code component_id} column is not used — violations are correlated by pathname and hash within
 * IQ Server's own data model. The legacy {@code HostedRepositoryMonitor} included this stamping
 * but it was only relevant for NXRM-connected repositories; this processor intentionally drops it
 * as out of scope for CLM-40039's IQ-Server-native continuous monitoring.
 */
@Named
@Singleton
public class RepositoryContinuousMonitoringFlowProcessor
    implements ContinuousMonitoringFlowProcessor
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryContinuousMonitoringFlowProcessor.class);

  private final ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  @Inject
  public RepositoryContinuousMonitoringFlowProcessor(
      final ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyEvaluator repositoryPolicyEvaluator)
  {
    this.hostedRepoItemDAO = hostedRepoItemDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
  }

  @Override
  public ContinuousMonitoringFlowType getFlowType() {
    return ContinuousMonitoringFlowType.HOSTED_REPO;
  }

  /**
   * Defense-in-depth drop branches in this method intentionally {@code return} (rather than
   * throw) when the row cannot be evaluated due to data drift — missing satellite, missing
   * repository, format change, monitoring disabled mid-cycle, etc. This routes through
   * {@code AbstractPollDispatchQueueConsumer.processJob}'s normal-return path → consumer's
   * {@code onJobSuccess} → {@code queueDAO.deleteById} (the queue-disposal lifecycle), which is
   * the desired terminal state for these items: they are not retryable, but they have also not
   * "failed" in the retry-and-back-off sense. Log lines at WARN make the drops visible to
   * operators. If a future "successful evaluations" metric is wired up, it should distinguish
   * drops from real evaluations rather than counting both as success.
   */
  @Override
  public void process(final ContinuousMonitoringQueueItem item) {
    String queueId = item.getId();
    ContinuousMonitoringHostedRepoItem satellite = loadSatellite(queueId);
    if (satellite == null) {
      log.warn("Continuous monitoring (hosted_repo): satellite missing for queueId={}; dropping.", queueId);
      return;
    }
    String repositoryId = satellite.getRepositoryId();
    if (repositoryId == null) {
      // Satellite columns are NOT NULL at the schema level, but defend against schema drift /
      // raw inserts: Set.of(null) throws NPE (CLM-37961-style), so handle missing data cleanly.
      log.warn("Continuous monitoring (hosted_repo): satellite has null repository_id for queueId={}; dropping.",
          queueId);
      return;
    }
    String componentHash = satellite.getComponentHash();
    if (componentHash == null) {
      log.warn("Continuous monitoring (hosted_repo): satellite has null component_hash for queueId={}; dropping.",
          queueId);
      return;
    }

    Repository repository = repositoryDAO.getById(repositoryId);
    if (repository == null) {
      log.info("Continuous monitoring (hosted_repo): repository {} no longer exists; dropping queueId={}.",
          repositoryId, queueId);
      return;
    }
    // Defense in depth: the producer cycle filtered on these conditions, but state can change
    // between enqueue and consume. Skip cleanly rather than evaluating a no-longer-eligible repo.
    if (RepositoryType.hosted != repository.getRepositoryType()
        || !repository.isMonitoringEnabled())
    {
      log.info("Continuous monitoring (hosted_repo): repository {} not eligible (type={}, monitoringEnabled={});"
          + " dropping queueId={}.",
          repository.getId(), repository.getRepositoryType(), repository.isMonitoringEnabled(), queueId);
      return;
    }

    List<RepositoryComponent> components =
        repositoryComponentDAO.getByRepositoryIdAndHash(repository.getId(), componentHash);
    if (components.isEmpty()) {
      log.info("Continuous monitoring (hosted_repo): no components for repository={} hash={}; dropping queueId={}.",
          repository.getId(), componentHash, queueId);
      return;
    }

    String repoFormat = repository.getFormat();
    if (repoFormat == null) {
      log.warn("Continuous monitoring (hosted_repo): repository {} has no format; dropping queueId={}.",
          repository.getId(), queueId);
      return;
    }

    String stage = components.stream()
        .map(RepositoryComponent::getLastEvaluationStage)
        .filter(s -> s != null)
        .findFirst()
        .orElse(ComplianceStageType.ID);

    RepositoryComponentEvaluationDataRequestList request =
        new RepositoryComponentEvaluationDataRequestList(RepositoryPolicyEvaluator.CONTINUOUS_MONITORING_CAUSE);
    for (RepositoryComponent component : components) {
      if (component.getHash() != null && component.getPathname() != null) {
        request.components.add(new RepositoryComponentEvaluationDataRequest(
            repoFormat,
            component.getPathname(),
            component.getHash()));
      }
    }
    if (request.components.isEmpty()) {
      log.info(
          "Continuous monitoring (hosted_repo): no evaluatable components for repository={}, hash={}, queueId={}; dropping.",
          repository.getId(), componentHash, queueId);
      return;
    }

    repositoryPolicyEvaluator.evaluateForMonitoring(repository, request, stage);
  }

  private ContinuousMonitoringHostedRepoItem loadSatellite(final String queueId) {
    try (TransactionContext tx = hostedRepoItemDAO.createTransactionContext()) {
      List<ContinuousMonitoringHostedRepoItem> rows = hostedRepoItemDAO.getByQueueIds(tx, List.of(queueId));
      return rows.isEmpty() ? null : rows.get(0);
    }
  }
}
