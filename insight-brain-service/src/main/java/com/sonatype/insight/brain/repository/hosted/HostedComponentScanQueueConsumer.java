/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.service.AdminTask;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.queue.AbstractPollDispatchQueueConsumer;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-tenant worker pool that processes the hosted component scan queue.
 * <p>
 * Extends {@link AbstractPollDispatchQueueConsumer} for the Poll-and-Dispatch pattern
 * and implements {@link ConfigurationListener} for live configuration updates.
 * Implements {@link AdminTask} for manual triggering via
 * {@code POST /tasks/HostedComponentScanQueueConsumer} on the admin port.
 * <p>
 * Each tenant gets its own isolated thread pool. Within a tenant, jobs are processed serially
 * (1 worker thread by default) — tenants never block each other.
 */
@Named
@Singleton
public class HostedComponentScanQueueConsumer
    extends AbstractPollDispatchQueueConsumer<HostedComponentScanQueue>
    implements ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(HostedComponentScanQueueConsumer.class);

  public static final String PATH = "HostedComponentScanQueueConsumer";

  private static final String CONSUMER_NAME = PATH;

  /**
   * Soft warning threshold for the per-archive component count. We don't enforce a hard cap
   * because silently truncating inner findings would hide real CVEs. The number is large enough
   * to never trip on normal application archives (~5–50 inner jars) but small enough that
   * anything above it is worth a logged note for ops correlation.
   */
  private static final int HIGH_COMPONENT_COUNT_THRESHOLD = 500;

  private final ApiConfigurationService apiConfigurationService;

  private final HostedComponentScanQueueDAO scanQueueDAO;

  private final Provider<ScanPersistenceService> scanPersistenceServiceProvider;

  private final Provider<ScanUploader> scanUploaderProvider;

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final Provider<ScanPolicyEvaluator> scanPolicyEvaluatorProvider;

  private final HostedRepositoryComponentResolver resolver;

  final TenantReference<HostedComponentScanQueueConfig> configs;

  @Inject
  public HostedComponentScanQueueConsumer(
      final ApiConfigurationService apiConfigurationService,
      final HostedComponentScanQueueDAO scanQueueDAO,
      final Provider<ScanPersistenceService> scanPersistenceServiceProvider,
      final Provider<ScanUploader> scanUploaderProvider,
      final RepositoryDAO repositoryDAO,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final Provider<ScanPolicyEvaluator> scanPolicyEvaluatorProvider,
      final HostedRepositoryComponentResolver resolver,
      final ShutdownHandler shutdownHandler)
  {
    super(CONSUMER_NAME, shutdownHandler);
    this.apiConfigurationService = apiConfigurationService;
    this.scanQueueDAO = scanQueueDAO;
    this.scanPersistenceServiceProvider = scanPersistenceServiceProvider;
    this.scanUploaderProvider = scanUploaderProvider;
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.scanPolicyEvaluatorProvider = scanPolicyEvaluatorProvider;
    this.resolver = resolver;
    this.configs = new TenantReference<>(this::loadConfig);
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (!propertyNames.contains(SystemConfigurationProperty.HOSTED_SCAN_QUEUE_CONFIG)) {
      return;
    }
    HostedComponentScanQueueConfig oldConfig = configs.get();
    HostedComponentScanQueueConfig newConfig = loadConfig();
    configs.set(newConfig);
    handleConfigurationChanged(
        newConfig.workerThreadsPerTenant(),
        newConfig.pollIntervalMilliseconds(),
        newConfig.enabled(),
        oldConfig.pollIntervalMilliseconds(),
        oldConfig.enabled());
  }

  @Override
  protected boolean isEnabled() {
    return configs.get().enabled();
  }

  @Override
  protected void recoverStaleJobs() {
    try {
      int reset = scanQueueDAO.resetInProgressToPending();
      if (reset > 0) {
        log.info("Reset {} stale IN_PROGRESS hosted component scan jobs to PENDING on startup", reset);
      }
    }
    catch (Exception e) {
      log.error("Failed to reset stale hosted component scan jobs on startup", e);
    }
  }

  @Override
  protected List<HostedComponentScanQueue> acquireJobs(final int limit) {
    return scanQueueDAO.acquireNextPendingJobs(limit);
  }

  @Override
  protected String getJobId(final HostedComponentScanQueue job) {
    return job.getId();
  }

  @Override
  protected void executeJob(final HostedComponentScanQueue job) throws Exception {
    String repositoryId = job.getRepositoryId();

    // A queued job can outlive its repo's monitoring being disabled or the repo being deleted; drop it
    // rather than scan a stale repo.
    Repository repository = repositoryDAO.getById(repositoryId);
    if (repository == null) {
      log.info("Hosted component scan: repository {} no longer exists; dropping scan job id={}.",
          repositoryId, job.getId());
      return;
    }
    if (!repository.isMonitoringEnabled()) {
      log.info("Hosted component scan: repository {} monitoring disabled; dropping scan job id={}.",
          repositoryId, job.getId());
      return;
    }

    ScanEntity scanEntity =
        scanPersistenceServiceProvider.get().getScanByName(repositoryId, job.getScanFileId());
    if (scanEntity == null) {
      throw new IllegalStateException(
          "Scan file not found: repositoryId=" + repositoryId + ", scanFileId=" + job.getScanFileId());
    }

    List<ScanComponentInfo> componentInfos = ScanXmlParser.extractComponentInfos(scanEntity);
    if (componentInfos.size() > HIGH_COMPONENT_COUNT_THRESHOLD) {
      log.warn("Archive scan job id={} unpacked into {} components (threshold={}). Processing all but "
          + "this is unusual; investigate if it correlates with degraded eval performance.",
          job.getId(), componentInfos.size(), HIGH_COMPONENT_COUNT_THRESHOLD);
    }

    String stage = normalizeStage(job.getPolicyEvaluationStage(), job.getId());

    if (componentInfos.isEmpty()) {
      // Preserve HDS audit trail for scans the scanner couldn't parse: upload via the repository
      // pipeline so HDS still has the raw scan, then bail without evaluation.
      ScanReceipt scanReceipt = scanUploaderProvider.get()
          .upload(scanEntity, repository, stage, null, null, true);
      log.warn("Could not extract any component info from scan file for job id={}; uploaded via repository pipeline"
          + " (scanId={}) and skipping policy evaluation.",
          job.getId(), scanReceipt.getScanId());
      return;
    }

    ScanComponentInfo outer = componentInfos.get(0);

    HostedRepositoryComponent hrc = resolver.getOrCreate(
        repositoryId, outer.pathname(), outer.hash(), job.getComponentId());

    ScanReceipt receipt = scanUploaderProvider.get()
        .upload(scanEntity, hrc, stage, null, null, true);

    // Manual Re-Evaluate and Continuous Monitoring both re-read this via
    // ScanPersistenceService.getScan(hrc.getId(), scanId).
    storeScanForReEvaluate(scanEntity, hrc.getId(), receipt.getScanId());

    scanPolicyEvaluatorProvider.get()
        .evaluate(
            hrc, receipt.getScanId(), new Stage(stage.toLowerCase()),
            ScanTriggerType.HOSTED_REPOSITORY_SCANNING, ClientScanType.SONATYPE, false);

    resolver.pinOwnerComponent(hrc, receipt.getScanId(), stage.toLowerCase());
  }

  /**
   * Canonicalizes the raw {@code policy_evaluation_stage} column value pulled from the NXRM
   * scan-queue payload into an IQ canonical stage id.
   * <p>
   * NXRM historically sends stage in inconsistent shapes (per production DB survey
   * 2026-07-01): {@code RELEASE} / {@code release} (upper/lower case), {@code BUILD},
   * {@code STAGE_RELEASE} (underscore instead of hyphen), and occasionally {@code NULL}.
   * IQ's own stage IDs (see {@code Stage.ID_BUILD}, {@code Stage.ID_STAGE_RELEASE} etc.) are
   * all lower-case with hyphens. Feeding an un-normalized value like {@code stage_release}
   * (underscore) into policy evaluation silently mis-routes the scan because it doesn't
   * match any registered stage id.
   * <p>
   * Normalization steps:
   * <ol>
   * <li>NULL / blank → {@link ComplianceStageType#ID} fallback (existing behaviour, but now
   * explicitly logged at WARN so ops can correlate with NXRM configs that aren't
   * propagating the stage — root cause of the 29,952 stage-release vs 124 build
   * telemetry imbalance Dariush flagged).</li>
   * <li>Lowercase — {@code RELEASE} → {@code release}, {@code BUILD} → {@code build}.</li>
   * <li>Replace underscores with hyphens — {@code stage_release} → {@code stage-release}.
   * This is the fix for the {@code STAGE_RELEASE} value NXRM sometimes sends.</li>
   * </ol>
   * <p>
   * Non-goal: this method does NOT validate the canonicalized value against known
   * {@code Stage.ID_*} constants. The downstream policy evaluator already handles unknown
   * stage ids gracefully (treats them as no-match); adding validation here would risk
   * failing scans on stage values that IQ silently tolerates today.
   */
  @VisibleForTesting
  static String normalizeStage(final String rawStage, final String jobId) {
    if (rawStage == null || rawStage.isBlank()) {
      log.warn("CLM-42079: Hosted scan job id={} has NULL/blank policy_evaluation_stage; "
          + "defaulting to {}. This usually indicates NXRM is not sending the configured stage "
          + "on the /rest/repositories/hosted/scan payload.", jobId, ComplianceStageType.ID);
      return ComplianceStageType.ID;
    }
    return rawStage.toLowerCase().replace('_', '-');
  }

  private void storeScanForReEvaluate(final ScanEntity scanEntity, final String appId, final String scanId) {
    try {
      ScanEntity tempScan = scanPersistenceServiceProvider.get().createTempScan(appId);
      scanPersistenceServiceProvider.get().copyScanFile(scanEntity, tempScan);
      scanPersistenceServiceProvider.get().moveTempScan(tempScan, appId, scanId);
      log.debug("Stored scan for re-evaluate: appId={} scanId={}", appId, scanId);
    }
    catch (Exception e) {
      log.warn("Failed to store scan for re-evaluate appId={} scanId={}: {}", appId, scanId, e.getMessage(), e);
    }
  }

  @Override
  protected void onJobSuccess(final HostedComponentScanQueue job) {
    scanQueueDAO.completeJob(job.getId());
    try {
      ScanEntity scanEntity =
          scanPersistenceServiceProvider.get().getScanByName(job.getRepositoryId(), job.getScanFileId());
      if (scanEntity != null) {
        scanPersistenceServiceProvider.get().deleteScan(scanEntity);
      }
    }
    catch (Exception e) {
      log.warn("Failed to clean up scan file scanFileId={} for completed job id={}",
          job.getScanFileId(), job.getId(), e);
    }
  }

  @Override
  protected int incrementRetryCount(final HostedComponentScanQueue job) {
    return scanQueueDAO.incrementRetryCount(job.getId());
  }

  @Override
  protected void unacquireJobs(final Set<String> ids) {
    scanQueueDAO.unacquireJobs(ids);
  }

  @Override
  protected void permanentlyFailJob(final HostedComponentScanQueue job, final Exception cause) {
    String errorMessage = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName();
    scanQueueDAO.failJob(job.getId(), errorMessage);
    try {
      ScanEntity scanEntity =
          scanPersistenceServiceProvider.get().getScanByName(job.getRepositoryId(), job.getScanFileId());
      if (scanEntity != null) {
        scanPersistenceServiceProvider.get().deleteScan(scanEntity);
      }
    }
    catch (Exception e) {
      log.warn("Failed to clean up scan file scanFileId={} for permanently failed job id={}",
          job.getScanFileId(), job.getId(), e);
    }
  }

  @Override
  protected int getWorkerThreadCount() {
    return configs.get().workerThreadsPerTenant();
  }

  @Override
  protected int getMaxQueuedRows() {
    return configs.get().maxQueuedRows();
  }

  @Override
  protected long getPollIntervalMs() {
    return configs.get().pollIntervalMilliseconds();
  }

  @Override
  protected int getMaxRetries() {
    return configs.get().maxRetries();
  }

  @Override
  protected String getConsumerName() {
    return CONSUMER_NAME;
  }

  @Override
  protected String getJitterSeed() {
    return ApplicationLifecycle.getServerInstanceId() + TenantThreadLocal.getTenant().tenantSlug;
  }

  private HostedComponentScanQueueConfig loadConfig() {
    Object raw = apiConfigurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.HOSTED_SCAN_QUEUE_CONFIG);
    return raw instanceof HostedComponentScanQueueConfig cfg
        ? cfg
        : HostedComponentScanQueueConfig.defaultConfig();
  }
}
