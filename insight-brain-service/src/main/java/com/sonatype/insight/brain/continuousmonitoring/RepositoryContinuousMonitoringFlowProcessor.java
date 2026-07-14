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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.hosted.ApplicationForHostedRepositoryComponentService;
import com.sonatype.insight.dataaccess.TransactionContext;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;

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
 * <b>CLM-42136:</b> after the outer evaluation, {@link #refreshHostedComponentOverlays} mirrors
 * nested-component violations and regenerates the on-disk Build Report overlay files
 * ({@code policythreats.json}, {@code bom.json}, {@code data.json}) so the Application Report
 * page and Latest Evaluations feed reflect the freshly-evaluated state rather than the initial
 * scan snapshot.
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

  /** Metric name for drop-branch counts (CLM-40971). Tagged with {@code reason}. */
  static final String DROP_METRIC_NAME = "insight_brain_cm_hosted_repo_drop_total";

  private final ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final ReportService reportService;

  private final ApplicationForHostedRepositoryComponentService applicationForHostedRepositoryComponentService;

  private final MeterRegistry meterRegistry;

  @Inject
  public RepositoryContinuousMonitoringFlowProcessor(
      final ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      final ReportService reportService,
      final ApplicationForHostedRepositoryComponentService applicationForHostedRepositoryComponentService,
      @Nullable final MeterRegistry meterRegistry)
  {
    this.hostedRepoItemDAO = hostedRepoItemDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.reportService = reportService;
    this.applicationForHostedRepositoryComponentService = applicationForHostedRepositoryComponentService;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Records a drop-branch hit on the metrics counter (CLM-40971). Each drop reason is a separate
   * tag value so operators can distinguish data-integrity patterns (e.g. recurring null
   * satellite columns) from successful evaluations and from one-off transient drops (e.g.
   * monitoring toggled off mid-cycle). Null-safe — if no MeterRegistry was wired (legacy code
   * path) the counter is a no-op.
   */
  private void recordDrop(final String reason) {
    if (meterRegistry != null) {
      meterRegistry.counter(DROP_METRIC_NAME, "reason", reason).increment();
    }
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
      recordDrop("satellite-missing");
      return;
    }
    String repositoryId = satellite.getRepositoryId();
    if (repositoryId == null) {
      // Satellite columns are NOT NULL at the schema level, but defend against schema drift /
      // raw inserts: Set.of(null) throws NPE (CLM-37961-style), so handle missing data cleanly.
      log.warn("Continuous monitoring (hosted_repo): satellite has null repository_id for queueId={}; dropping.",
          queueId);
      recordDrop("satellite-null-repository-id");
      return;
    }
    String componentHash = satellite.getComponentHash();
    if (componentHash == null) {
      log.warn("Continuous monitoring (hosted_repo): satellite has null component_hash for queueId={}; dropping.",
          queueId);
      recordDrop("satellite-null-component-hash");
      return;
    }

    Repository repository = repositoryDAO.getById(repositoryId);
    if (repository == null) {
      log.info("Continuous monitoring (hosted_repo): repository {} no longer exists; dropping queueId={}.",
          repositoryId, queueId);
      recordDrop("repository-deleted");
      return;
    }
    // Defense in depth: the producer cycle filtered on these conditions, but state can change
    // between enqueue and consume. Split into two branches so operators can distinguish
    // a configuration error (proxy/group repo enqueued) from an intentional state change
    // (monitoring disabled after enqueue).
    if (RepositoryType.hosted != repository.getRepositoryType()) {
      log.info("Continuous monitoring (hosted_repo): repository {} not hosted (type={}); dropping queueId={}.",
          repository.getId(), repository.getRepositoryType(), queueId);
      recordDrop("repository-not-hosted");
      return;
    }
    if (!repository.isMonitoringEnabled()) {
      log.info("Continuous monitoring (hosted_repo): repository {} monitoring disabled; dropping queueId={}.",
          repository.getId(), queueId);
      recordDrop("monitoring-disabled");
      return;
    }

    List<RepositoryComponent> components =
        repositoryComponentDAO.getByRepositoryIdAndHash(repository.getId(), componentHash);
    if (components.isEmpty()) {
      log.info("Continuous monitoring (hosted_repo): no components for repository={} hash={}; dropping queueId={}.",
          repository.getId(), componentHash, queueId);
      recordDrop("no-components-for-hash");
      return;
    }

    String repoFormat = repository.getFormat();
    if (repoFormat == null) {
      log.warn("Continuous monitoring (hosted_repo): repository {} has no format; dropping queueId={}.",
          repository.getId(), queueId);
      recordDrop("repository-no-format");
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
      recordDrop("no-evaluatable-components");
      log.info(
          "Continuous monitoring (hosted_repo): no evaluatable components for repository={}, hash={}, queueId={}; dropping.",
          repository.getId(), componentHash, queueId);
      return;
    }

    repositoryPolicyEvaluator.evaluateForMonitoring(repository, request, stage);

    refreshHostedComponentOverlays(repository, components, stage);
  }

  /**
   * Post-evaluation Build Report refresh (CLM-42136). Runs after
   * {@link RepositoryPolicyEvaluator#evaluateForMonitoring} has refreshed the outer's
   * {@code repository_policy_violation} rows. For each component in the batch, delegates to
   * {@link ReportService#refreshHostedComponentAfterEvaluation} which mirrors nested-component
   * violations and regenerates the on-disk overlay files ({@code policythreats.json},
   * {@code bom.json}, {@code data.json}). Without this step the outer's report would carry
   * stale inner-pathname violations from the initial scan.
   * <p>
   * Per-component failures are logged, counted on the drop meter, and swallowed — a single
   * failing component must not poison the rest of the batch, and the queue item's outer
   * evaluation has already succeeded.
   */
  private void refreshHostedComponentOverlays(
      final Repository repository,
      final List<RepositoryComponent> components,
      final String stage)
  {
    for (RepositoryComponent component : components) {
      String componentScanId = component.getScanId();
      String componentPathname = component.getPathname();
      if (componentScanId == null || componentPathname == null) {
        // Component is missing the identifiers we need to address its overlay files on disk.
        // Log + drop-meter so this doesn't hide silently if it starts happening at scale.
        log.info("Continuous monitoring (hosted_repo): skipping overlay refresh for repository={} "
            + "pathname={} scanId={}; missing identifier.",
            repository.getId(), componentPathname, componentScanId);
        recordDrop("overlay-refresh-missing-identifier");
        continue;
      }
      try {
        Application application = applicationForHostedRepositoryComponentService
            .getOrCreateApplication(repository.getId(), componentPathname);
        if (application == null) {
          // getOrCreateApplication returns null when the repository has no valid organization
          // (root-org lookup would fail). Distinguishing this from a mid-refresh exception keeps
          // the drop reason unambiguous for operators.
          log.warn("Continuous monitoring (hosted_repo): synthetic application unavailable for "
              + "repository={} pathname={} scanId={}; skipping overlay refresh.",
              repository.getId(), componentPathname, componentScanId);
          recordDrop("overlay-refresh-no-application");
          continue;
        }
        // persistPolicyEvaluationRow=false — the mirror step invoked by
        // refreshHostedComponentAfterEvaluation already persists a policy_evaluation row via
        // ScanPolicyEvaluator; the extra insert this flag controls is only useful on the
        // Re-Evaluate button path.
        reportService.refreshHostedComponentAfterEvaluation(
            component, repository, application, application.getId(), componentScanId, stage, false);
      }
      catch (Exception e) {
        log.warn("Continuous monitoring (hosted_repo): post-evaluation refresh failed for repository={}"
            + " pathname={} scanId={}; DB is fresh but Build Report files remain stale until next refresh.",
            repository.getId(), componentPathname, componentScanId, e);
        recordDrop("overlay-refresh-failed");
      }
    }
  }

  private ContinuousMonitoringHostedRepoItem loadSatellite(final String queueId) {
    try (TransactionContext tx = hostedRepoItemDAO.createTransactionContext()) {
      List<ContinuousMonitoringHostedRepoItem> rows = hostedRepoItemDAO.getByQueueIds(tx, List.of(queueId));
      return rows.isEmpty() ? null : rows.get(0);
    }
  }
}
