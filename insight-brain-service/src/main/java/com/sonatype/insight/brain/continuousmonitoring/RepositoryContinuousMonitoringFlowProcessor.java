/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringHostedRepoItemDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.repository.hosted.HostedRepositoryComponentResolver;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.scan.model.ClientScanType;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ContinuousMonitoringFlowProcessor} for the Hosted Repo flow (CLM-40039 Section 6.3).
 * Resolves the satellite row to (repositoryId, componentHash) and re-asserts that the repository is
 * a monitoring-enabled hosted repository owned by the current tenant (defense in depth on the
 * shared executor surface — Section 4.3 / STRIDE Section 9).
 * <p>
 * Each component matching the hash is then re-evaluated independently: a
 * {@link HostedRepositoryComponent} owner is resolved (get-or-create) via
 * {@link HostedRepositoryComponentResolver}, the scan stored under that owner is cloned into a temp
 * entity and uploaded to HDS as a fresh primary scan, and
 * {@link ScanPolicyEvaluator#evaluateForMonitoring} evaluates the fresh scanId against the owner —
 * the same Drools pipeline Lifecycle applications use. The evaluator updates
 * {@code last_evaluation_time} as part of evaluation. Finally
 * {@link HostedRepositoryComponentResolver#pinOwnerComponent} stamps {@code owner_component_id} on
 * the resulting violations.
 * <p>
 * A failure on one component is confined to that component: each stage records a distinct
 * {@code cm-*} drop metric and continues with the next component rather than aborting the batch.
 * <p>
 * Note: This processor does NOT call {@code stampComponentId} on the
 * {@code proxy_repository_policy_violation} table. That method is specific to NXRM-hosted repositories
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

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  private final MeterRegistry meterRegistry;

  private final HostedRepositoryComponentResolver resolver;

  private final ScanPersistenceService scanPersistenceService;

  private final ScanUploader scanUploader;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  public RepositoryContinuousMonitoringFlowProcessor(
      final ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO,
      final RepositoryDAO repositoryDAO,
      final HostedRepositoryComponentDAO hostedRepositoryComponentDAO,
      @Nullable final MeterRegistry meterRegistry,
      final HostedRepositoryComponentResolver resolver,
      final ScanPersistenceService scanPersistenceService,
      final ScanUploader scanUploader,
      final ScanPolicyEvaluator scanPolicyEvaluator,
      final PolicyEvaluationDAO policyEvaluationDAO)
  {
    this.hostedRepoItemDAO = hostedRepoItemDAO;
    this.repositoryDAO = repositoryDAO;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
    this.meterRegistry = meterRegistry;
    this.resolver = resolver;
    this.scanPersistenceService = scanPersistenceService;
    this.scanUploader = scanUploader;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyEvaluationDAO = policyEvaluationDAO;
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

    List<HostedRepositoryComponent> components;
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      components = hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, repository.getId(), componentHash);
    }
    if (components.isEmpty()) {
      log.info("Continuous monitoring (hosted_repo): no components for repository={} hash={}; dropping queueId={}.",
          repository.getId(), componentHash, queueId);
      recordDrop("no-components-for-hash");
      return;
    }

    // The artifact's scan history lives in policy_evaluation, so the scan to re-upload and the stage to
    // evaluate it against both come from its latest evaluation. Fetched for the whole batch up front:
    // getLastByOwnerIds takes a Set precisely so this costs one round trip rather than one per component
    // (any number of pathnames can share a content hash, so the batch is unbounded in principle).
    //
    // last_policy_evaluation is unique on (owner_id, stage_type_id), so an artifact uploaded at more than
    // one stage — the stage is per-request on the NXRM scan payload — has one row per stage here. Keep the
    // most recent by evaluation time so the stage this cycle refreshes is the one the artifact was
    // evaluated at last, rather than whichever row the unordered query happened to return.
    Map<String, PolicyEvaluation> lastEvaluationsByOwnerId = policyEvaluationDAO
        .getLastByOwnerIds(components.stream().map(HostedRepositoryComponent::getId).collect(Collectors.toSet()))
        .stream()
        .filter(e -> e.getOwnerId() != null)
        .collect(Collectors.toMap(
            PolicyEvaluation::getOwnerId,
            e -> e,
            BinaryOperator.maxBy(Comparator.comparing(
                PolicyEvaluation::getTime,
                Comparator.nullsFirst(Comparator.naturalOrder())))));

    for (HostedRepositoryComponent hrc : components) {
      String componentPathname = hrc.getPathname();

      // An artifact with no evaluation yet has nothing to monitor — the same condition PolicyMonitor
      // treats as "nothing to monitor" for an application with no scan for its monitored stage.
      PolicyEvaluation lastEvaluation = lastEvaluationsByOwnerId.get(hrc.getId());
      if (lastEvaluation == null || lastEvaluation.getScanId() == null) {
        log.info("Continuous monitoring (hosted_repo): no evaluation to monitor for repository={} pathname={}; "
            + "skipping.", repository.getId(), componentPathname);
        recordDrop("cm-no-previous-evaluation");
        continue;
      }
      String componentScanId = lastEvaluation.getScanId();
      String stage = lastEvaluation.getStageTypeId() != null
          ? lastEvaluation.getStageTypeId()
          : ComplianceStageType.ID;

      ScanEntity tempScanEntity = null;
      try {
        try {
          tempScanEntity = scanPersistenceService.createTempScan(hrc.getId());
        }
        catch (Exception e) {
          log.warn("Continuous monitoring (hosted_repo): temp-scan create failed for repository={} "
              + "pathname={}; skipping. err={}", repository.getId(), componentPathname, e.getMessage(), e);
          recordDrop("cm-temp-scan-create-failed");
          continue;
        }
        try {
          cloneLatestScanFile(tempScanEntity, hrc, componentScanId, stage);
        }
        catch (Exception e) {
          log.warn("Continuous monitoring (hosted_repo): clone-scan failed for repository={} pathname={} "
              + "scanId={}; skipping. err={}",
              repository.getId(), componentPathname, componentScanId, e.getMessage(), e);
          recordDrop("cm-clone-scan-failed");
          continue;
        }

        ScanReceipt receipt;
        try {
          receipt = scanUploader.upload(tempScanEntity, hrc, stage, null, null, true);
        }
        catch (Exception e) {
          log.warn("Continuous monitoring (hosted_repo): scan-upload failed for repository={} pathname={} "
              + "scanId={}; skipping. err={}",
              repository.getId(), componentPathname, componentScanId, e.getMessage(), e);
          recordDrop("cm-upload-failed");
          continue;
        }

        String freshScanId = receipt.getScanId();
        try {
          scanPolicyEvaluator.evaluateForMonitoring(
              hrc, freshScanId, new Stage(stage),
              ScanTriggerType.HOSTED_REPOSITORY_SCANNING, ClientScanType.SONATYPE);
        }
        catch (Exception e) {
          log.warn("Continuous monitoring (hosted_repo): evaluation failed for repository={} pathname={} "
              + "freshScanId={}; skipping. err={}",
              repository.getId(), componentPathname, freshScanId, e.getMessage(), e);
          recordDrop("cm-evaluation-failed");
          continue;
        }

        resolver.pinOwnerComponent(hrc, freshScanId, stage);

        // Finalize the clone under the fresh scanId rather than discarding it. The evaluation just
        // persisted names this scanId, and both the next monitoring cycle and Manual Re-Evaluate read
        // the scan back by it — a pointer to a deleted file leaves monitoring stranded and makes
        // Re-Evaluate fail on a missing scan. Ownership transfers to the datastore here, so the
        // finally-block delete must not also run: null the local reference on success.
        scanPersistenceService.moveTempScan(tempScanEntity, hrc.getId(), freshScanId);
        tempScanEntity = null;

      }
      catch (Exception e) {
        log.warn("Continuous monitoring (hosted_repo): unexpected failure for repository={} pathname={}; "
            + "skipping. err={}", repository.getId(), componentPathname, e.getMessage(), e);
        recordDrop("cm-unexpected-failure");
      }
      finally {
        if (tempScanEntity != null) {
          try {
            scanPersistenceService.deleteScan(tempScanEntity);
          }
          catch (Exception e) {
            log.warn("Continuous monitoring (hosted_repo): failed to delete cloned scan for repository={} "
                + "pathname={}; err={}", repository.getId(), componentPathname, e.getMessage(), e);
          }
        }
      }
    }
  }

  /**
   * Clones the artifact's stored {@code scan.xml.gz} into {@code tempScanEntity}, following the pointer
   * forward if a concurrent evaluation moved it.
   * <p>
   * Every primary evaluation deletes the scan file of the evaluation it supersedes
   * ({@code ScanPolicyEvaluator.deletePreviousScanFile}), so a monitoring cycle can find the file it
   * intended to copy already gone — the id was read before the copy, and an upload or another cycle can
   * land in between. Rather than dropping the component for a cycle, re-read the owner's latest primary
   * evaluation and copy that instead; only a source that is still the latest is a genuine failure. This
   * mirrors {@code PolicyMonitor.cloneScanFile}, which handles the same race on the Lifecycle path.
   *
   * @param tempScanEntity the temp entity to copy into
   * @param hrc the artifact being monitored, and the owner the scan is stored under
   * @param scanId the scan the caller intended to clone
   * @param stageTypeId the stage whose latest primary evaluation identifies a newer scan
   */
  private void cloneLatestScanFile(
      final ScanEntity tempScanEntity,
      final HostedRepositoryComponent hrc,
      final String scanId,
      final String stageTypeId)
  {
    String sourceScanId = scanId;
    while (true) {
      try {
        scanPersistenceService.copyScanFile(scanPersistenceService.getScan(hrc.getId(), sourceScanId), tempScanEntity);
        return;
      }
      catch (Exception e) {
        PolicyEvaluation latest = policyEvaluationDAO.getLastPrimaryByOwnerIdAndStageId(hrc.getId(), stageTypeId);
        if (latest == null || latest.getScanId() == null || sourceScanId.equals(latest.getScanId())) {
          throw new IllegalStateException("Could not clone scan " + sourceScanId + " for hrcId=" + hrc.getId()
              + "; it is still the latest primary scan for stage " + stageTypeId, e);
        }
        log.debug("Continuous monitoring (hosted_repo): scan {} for hrcId={} was superseded mid-clone; "
            + "retrying with {}", sourceScanId, hrc.getId(), latest.getScanId());
        sourceScanId = latest.getScanId();
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
